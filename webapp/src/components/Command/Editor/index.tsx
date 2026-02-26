import React, { useCallback, useEffect, useState } from 'react';
import {
  Form,
  Modal,
  Input,
  Typography,
  Button,
  Select,
  Checkbox,
  Radio,
  message,
  Card,
  Space,
  Tag,
  Divider,
  Tabs,
  Empty,
  Tooltip,
  InputNumber,
  Alert,
} from 'antd';
import {
  CloseOutlined,
  PlusOutlined,
  CodeOutlined,
  SettingOutlined,
  EditOutlined,
  CloudServerOutlined,
} from '@ant-design/icons';
import { createCommand, updateCommand } from '@/services/httprun';
import styles from './index.module.less';

const { Text } = Typography;

export interface CommandEditorProps {
  open: boolean;
  command: HTTPRUN.CommandItem | Record<string, never>;
  onClose: () => void;
  onChange?: () => void;
  checkNameDuplicate?: (name: string) => boolean;
}

/** 获取命令配置，兼容旧版 command 字段 */
const getCommandConfig = (cmd: HTTPRUN.CommandItem | null | undefined): HTTPRUN.CommandConfig => {
  if (!cmd) return { command: '', params: [], env: [] };
  return cmd.commandConfig || cmd.command || { command: '', params: [], env: [] };
};

/** 获取远程配置，默认为本机 */
const getRemoteConfig = (cmd: HTTPRUN.CommandItem | null | undefined): HTTPRUN.RemoteConfig => {
  if (!cmd || !cmd.remoteConfig) return { host: 'localhost', port: 22 };
  return cmd.remoteConfig;
};

const CommandEditor: React.FC<CommandEditorProps> = ({
  open,
  onClose,
  command,
  onChange,
  checkNameDuplicate,
}) => {
  const [value, setValue] = useState<HTTPRUN.CommandItem>(
    (command as HTTPRUN.CommandItem) || { commandConfig: { command: '', params: [], env: [] }, remoteConfig: { host: 'localhost', port: 22 } },
  );
  const [form] = Form.useForm();
  const [loading, setLoading] = useState<boolean>(false);
  const [executionMode, setExecutionMode] = useState<HTTPRUN.ExecutionMode>('LOCAL');
  // SSH 主机模式：fixed=固定主机, param=参数化（执行时动态传入）
  const [hostMode, setHostMode] = useState<'fixed' | 'param'>('fixed');

  useEffect(() => {
    setValue(
      (command as HTTPRUN.CommandItem) || { commandConfig: { command: '', params: [], env: [] }, remoteConfig: { host: 'localhost', port: 22 } },
    );
    if (command && 'name' in command) {
      const cfg = getCommandConfig(command as HTTPRUN.CommandItem);
      const remote = getRemoteConfig(command as HTTPRUN.CommandItem);
      const mode = (command as HTTPRUN.CommandItem).executionMode || 'LOCAL';
      setExecutionMode(mode);
      // 检测 host 是否为参数化模板
      const isParamHost = remote.host && /^\{\{[^}]+\}\}$/.test(remote.host);
      setHostMode(isParamHost ? 'param' : 'fixed');
      form.setFieldsValue({
        name: command.name,
        description: command.description,
        command: cfg.command,
        host: isParamHost ? '' : (remote.host || 'localhost'),
        port: remote.port || 22,
        username: remote.username || '',
        // 脱敏值不回填，保持为空
        password: (remote.password && remote.password !== '******') ? remote.password : '',
        privateKey: (remote.privateKey && remote.privateKey !== '******') ? remote.privateKey : '',
      });
    } else {
      form.resetFields();
      setExecutionMode('LOCAL');
      setHostMode('fixed');
      form.setFieldsValue({
        host: 'localhost',
        port: 22,
      });
    }
  }, [command, form]);

  const isCopyMode = !!checkNameDuplicate;
  const isEdit = command && 'name' in command && command.name && !isCopyMode;
  const title = isCopyMode ? '复制命令' : isEdit ? '编辑命令' : '添加命令';

  const handleParamAdd = useCallback(() => {
    const newValue = { ...value } as HTTPRUN.CommandItem;
    if (!newValue.commandConfig) {
      newValue.commandConfig = {
        command: '',
        params: [],
        env: [],
      };
    }
    if (!newValue.commandConfig.params) {
      newValue.commandConfig.params = [];
    }
    newValue.commandConfig.params = [
      ...newValue.commandConfig.params,
      { name: '', description: '', defaultValue: '', required: true, type: 'string' },
    ];
    setValue(newValue);
  }, [value]);

  const handleEnvAdd = useCallback(() => {
    const newValue = { ...value } as HTTPRUN.CommandItem;
    if (!newValue.commandConfig) {
      newValue.commandConfig = {
        command: '',
        params: [],
        env: [],
      };
    }
    if (!newValue.commandConfig.env) {
      newValue.commandConfig.env = [];
    }
    newValue.commandConfig.env = [...newValue.commandConfig.env, { name: '', value: '' }];
    setValue(newValue);
  }, [value]);

  const handleEnvRemove = useCallback(
    (i: number) => {
      const newValue = { ...value } as HTTPRUN.CommandItem;
      const cfg = getCommandConfig(newValue);
      cfg.env = cfg.env.filter((_, index) => index !== i);
      newValue.commandConfig = cfg;
      setValue(newValue);
    },
    [value],
  );

  const handleParamRemove = useCallback(
    (i: number) => {
      const newValue = { ...value } as HTTPRUN.CommandItem;
      const cfg = getCommandConfig(newValue);
      cfg.params = cfg.params.filter((_, index) => index !== i);
      newValue.commandConfig = cfg;
      setValue(newValue);
    },
    [value],
  );

  const handleParamItemChange = useCallback(
    (i: number, name: keyof HTTPRUN.ParamDefine, val: any) => {
      const newValue = { ...value } as HTTPRUN.CommandItem;
      const cfg = getCommandConfig(newValue);
      cfg.params = cfg.params.map((param, index) =>
        index === i ? { ...param, [name]: val } : param,
      );
      newValue.commandConfig = cfg;
      setValue(newValue);
    },
    [value],
  );

  const handleEnvChange = useCallback(
    (i: number, type: 'name' | 'value', val: string) => {
      const newValue = { ...value } as HTTPRUN.CommandItem;
      const cfg = getCommandConfig(newValue);
      cfg.env = cfg.env.map((env, index) =>
        index === i ? { ...env, [type]: val } : env,
      );
      newValue.commandConfig = cfg;
      setValue(newValue);
    },
    [value],
  );

  const handleCreateCommand = useCallback(() => {
    form
      .validateFields()
      .then(() => {
        setLoading(true);
        const val = form.getFieldsValue();
        const cfg = getCommandConfig(value);
        cfg.command = val.command;
        
        // 构建请求对象，直接使用用户选择的executionMode
        const req: any = {
          name: val.name,
          description: val.description,
          commandConfig: cfg,
          executionMode: executionMode,  // 直接使用状态中的executionMode，不再自动判断
          status: 0,
          path: `/api/run/${val.name}`,
        };
        
        // 仅在 SSH 模式下添加 remoteConfig
        if (executionMode === 'SSH') {
          req.remoteConfig = {
            // 参数化模式：host 固定为 {{.host}}，执行时从 params 动态解析
            host: hostMode === 'param' ? '{{.host}}' : val.host,
            port: val.port || 22,
            username: val.username,
            // 编辑模式下，如果字段为空则不传（表示不修改）
            password: (isEdit && !val.password) ? undefined : val.password,
            privateKey: (isEdit && !val.privateKey) ? undefined : val.privateKey,
          };
          
        }
        
        const apiCall = isEdit
          ? updateCommand(val.name, req)
          : createCommand(req);
        
        apiCall
          .then(() => {
            message.success(isEdit ? '更新成功！' : '创建成功！');
            setLoading(false);
            if (onChange) onChange();
            if (onClose) onClose();
          })
          .catch((err) => {
            setLoading(false);
            message.error('操作失败: ' + err.message);
          });
      })
      .catch(() => {
        message.warning('请填写必填信息');
      });
  }, [form, value, onChange, onClose, isEdit, executionMode, hostMode]);

  const cfg = getCommandConfig(value);
  const paramItems = cfg.params || [];
  const envItems = cfg.env || [];

  return (
    <Modal
      open={open}
      title={
        <Space>
          <EditOutlined style={{ color: '#1677ff' }} />
          <span>{title}</span>
        </Space>
      }
      onCancel={onClose}
      okText={isEdit ? '更新' : '创建'}
      cancelText="取消"
      okButtonProps={{ loading, onClick: handleCreateCommand }}
      width={700}
      destroyOnClose
    >
      <div className={styles.editor}>
        {/* 基本信息 */}
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            name: value?.name,
            description: value?.description,
            command: cfg.command,
          }}
        >
          <div className={styles.formGrid}>
            <Form.Item
              label="命令名称"
              name="name"
              rules={[
                { required: true, message: '请输入命令名称' },
                ({ getFieldValue }) => ({
                  validator: (_, value) => {
                    if (!value) return Promise.resolve();
                    if (checkNameDuplicate && checkNameDuplicate(value)) {
                      return Promise.reject(new Error('命令名称已存在，请修改'));
                    }
                    return Promise.resolve();
                  },
                }),
              ]}
              tooltip="命令的唯一标识，用于 API 调用"
            >
              <Input placeholder="如：deploy-app" disabled={!!isEdit} />
            </Form.Item>
            <Form.Item
              label="命令描述"
              name="description"
              rules={[{ required: true, message: '请输入命令描述' }]}
              tooltip="命令的功能说明"
            >
              <Input placeholder="如：部署应用到生产环境" />
            </Form.Item>
          </div>
          
          {/* 执行模式选择 */}
          <Form.Item
            label="执行模式"
            required
            tooltip="本地执行：在服务器本地运行命令；SSH执行：通过SSH在远程服务器运行"
          >
            <Radio.Group 
              value={executionMode} 
              onChange={(e) => setExecutionMode(e.target.value)}
            >
              <Radio value="LOCAL">🏠 本地执行</Radio>
              <Radio value="SSH">🔐 SSH 远程执行</Radio>
            </Radio.Group>
          </Form.Item>
          
          {/* 根据执行模式显示不同配置 */}
          {executionMode === 'SSH' ? (
            <>
              {isEdit && (
                <Alert
                  message="密码和私钥不回显"
                  description="出于安全考虑，密码和私钥在编辑时不会显示。保持空白表示不修改，重新填写表示更新。"
                  type="info"
                  showIcon
                  closable
                  style={{ marginBottom: 16 }}
                />
              )}
              
              {/* 主机模式选择 */}
              <Form.Item
                label="主机模式"
                required
                tooltip="固定主机：配置固定 IP 地址；参数化主机：执行时动态传入，适用于批量节点执行场景"
              >
                <Radio.Group
                  value={hostMode}
                  onChange={(e) => {
                    const mode = e.target.value as 'fixed' | 'param';
                    setHostMode(mode);
                    form.setFieldsValue({ host: '' });

                    // 切换到参数化模式：立即向参数列表添加 host 参数（若不存在）
                    if (mode === 'param') {
                      setValue((prev) => {
                        const newVal = { ...prev } as HTTPRUN.CommandItem;
                        const cfg = newVal.commandConfig
                          ? { ...newVal.commandConfig, params: [...(newVal.commandConfig.params || [])] }
                          : { command: '', params: [], env: [] };
                        const hasHostParam = cfg.params.some((p: HTTPRUN.ParamDefine) => p.name === 'host');
                        if (!hasHostParam) {
                          cfg.params = [
                            { name: 'host', description: 'SSH 目标主机地址', type: 'string', defaultValue: undefined, required: true },
                            ...cfg.params,
                          ];
                        }
                        newVal.commandConfig = cfg;
                        return newVal;
                      });
                    } else {
                      // 切回固定模式：移除系统自动添加的 host 参数（描述匹配才删，避免误删用户自定义的参数）
                      setValue((prev) => {
                        const newVal = { ...prev } as HTTPRUN.CommandItem;
                        const cfg = newVal.commandConfig
                          ? { ...newVal.commandConfig, params: [...(newVal.commandConfig.params || [])] }
                          : { command: '', params: [], env: [] };
                        cfg.params = cfg.params.filter(
                          (p: HTTPRUN.ParamDefine) =>
                            !(p.name === 'host' && p.description === 'SSH 目标主机地址'),
                        );
                        newVal.commandConfig = cfg;
                        return newVal;
                      });
                    }
                  }}
                >
                  <Radio value="fixed">🖥️ 固定主机</Radio>
                  <Radio value="param">🔄 参数化主机（批量节点）</Radio>
                </Radio.Group>
              </Form.Item>

              {hostMode === 'param' ? (
                <Alert
                  message="参数化主机模式"
                  description={
                    <span>
                      主机地址将在执行时通过 <code>host</code> 参数动态传入。
                      系统会自动为你添加一个必填的 <strong>host</strong> 参数（不可删除）。
                      <br />
                      同一命令可对不同节点分别执行，无需重复配置命令。
                      <br />
                      <strong>注意：每次执行仅支持一台主机。</strong>若需在多台节点上执行，请对每台节点单独发起请求（可并发调用）。
                      <br />
                      <br />
                      <strong>示例——依次对多台节点执行：</strong>
                      <code style={{ display: 'block', marginTop: 4, padding: '4px 8px', background: '#f5f5f5', borderRadius: 4, whiteSpace: 'pre' }}>
                        {'# 节点一\ncurl -X POST /api/run/命令名 -d \'{"params":[{"name":"host","value":"192.168.1.10"}]}\'\n# 节点二\ncurl -X POST /api/run/命令名 -d \'{"params":[{"name":"host","value":"192.168.1.11"}]}\''}
                      </code>
                    </span>
                  }
                  type="info"
                  showIcon
                  style={{ marginBottom: 16 }}
                />
              ) : (
                <div className={styles.formGrid}>
                  <Form.Item
                    label="主机地址"
                    name="host"
                    rules={[
                      { required: true, message: '请输入主机地址' },
                      {
                        validator: (_, value) => {
                          if (!value) return Promise.resolve();
                          if (/^(localhost|127\.0\.0\.1|::1)$/i.test(value)) {
                            return Promise.reject('远程执行不能使用 localhost，请输入实际IP或域名');
                          }
                          return Promise.resolve();
                        }
                      }
                    ]}
                    tooltip="远程服务器的IP地址或域名"
                  >
                    <Input placeholder="如：192.168.1.100" />
                  </Form.Item>
                  <Form.Item
                    label="SSH 端口"
                    name="port"
                    initialValue={22}
                  >
                    <InputNumber min={1} max={65535} placeholder="22" style={{ width: '100%' }} />
                  </Form.Item>
                </div>
              )}

              <div className={styles.formGrid}>
                {hostMode === 'param' && (
                  <Form.Item
                    label="SSH 端口"
                    name="port"
                    initialValue={22}
                  >
                    <InputNumber min={1} max={65535} placeholder="22" style={{ width: '100%' }} />
                  </Form.Item>
                )}
                <Form.Item
                  label="用户名"
                  name="username"
                  rules={[{ required: true, message: '请输入SSH用户名' }]}
                  tooltip="远程服务器的登录用户名"
                >
                  <Input placeholder="如：root 或 ubuntu" />
                </Form.Item>
              </div>
              
              <Divider plain orientation="left" style={{ margin: '16px 0' }}>认证方式（三选一）</Divider>
              
              <Form.Item
                label="密码"
                name="password"
                tooltip="密码认证（最简单）。密码将被加密存储。"
              >
                <Input.Password 
                  placeholder={isEdit ? "留空表示不修改密码" : "SSH 登录密码"} 
                />
              </Form.Item>
              
              <Form.Item
                label="私钥"
                name="privateKey"
                tooltip="私钥认证（推荐）。支持 RSA、ECDSA、ED25519 等类型。"
              >
                <Input.TextArea
                  rows={4}
                  placeholder={
                    isEdit 
                      ? "留空表示不修改私钥" 
                      : "粘贴 PEM 格式的私钥内容\n-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----"
                  }
                  style={{ fontFamily: 'monospace', fontSize: 12 }}
                />
              </Form.Item>
              
              <Alert
                message="免密登录"
                description="如果不填写密码和私钥，系统会尝试使用服务器本地的 ~/.ssh/id_rsa 等默认密钥进行认证。"
                type="success"
                showIcon
                style={{ marginTop: 8, marginBottom: 16 }}
              />
            </>
          ) : (
            <Form.Item
              label="执行位置"
              tooltip="命令将在 HttpRun 服务器本地执行"
            >
              <Input value="本机 (localhost)" disabled />
            </Form.Item>
          )}
          
          <Divider />
          
          <Form.Item
            label="命令内容"
            name="command"
            rules={[
              { required: true, message: '请输入命令内容' },
              {
                validator: (_, value) => {
                  if (!value) return Promise.resolve();
                  // 先移除 \ 续行符（反斜杠+换行+可选空白），避免误判
                  // 这允许使用 \ 进行换行续行（Shell 语法）
                  const valueForCheck = value.replace(/\\\r?\n\s*/g, ' ');
                  
                  // 检测多命令连接符
                  const multiCommandPatterns = [
                    { pattern: /&&/, desc: '&& (命令连接符)' },
                    { pattern: /\|\|/, desc: '|| (条件执行符)' },
                    { pattern: /;(?![^{}]*})/, desc: '; (命令分隔符)' },
                    { pattern: /(?<!\|)\|(?!\|)/, desc: '| (管道符)' },
                    { pattern: /[\r\n]/, desc: '换行符（请使用 \\ 续行）' },
                    { pattern: /&(?!&)\s*$/, desc: '& (后台执行符)' },
                    { pattern: /&(?!&)\s+\S/, desc: '& (后台执行后跟命令)' },
                  ];
                  for (const { pattern, desc } of multiCommandPatterns) {
                    if (pattern.test(valueForCheck)) {
                      return Promise.reject(
                        new Error(`禁止使用多命令连接符: ${desc}。如需执行多条命令，请使用 Shell 脚本封装`)
                      );
                    }
                  }
                  return Promise.resolve();
                },
              },
            ]}
            tooltip="要执行的单条 Shell 命令，支持参数替换和 \\ 换行续行。禁止使用 &&、||、; 等多命令连接符"
          >
            <Input.TextArea
              rows={4}
              placeholder={'如：docker run \\\n  -d \\\n  --name {{.app_name}} \\\n  -p {{.port}}:8080 \\\n  {{.image}}'}
              style={{ fontFamily: 'monospace' }}
            />
          </Form.Item>
        </Form>

        <Divider style={{ margin: '16px 0' }} />

        {/* 参数和环境变量配置 */}
        <Tabs
          items={[
            {
              key: 'params',
              label: (
                <Space>
                  <SettingOutlined />
                  <span>参数配置</span>
                  {paramItems.length > 0 && <Tag>{paramItems.length}</Tag>}
                </Space>
              ),
              children: (
                <div className={styles.paramSection}>
                  {paramItems.length > 0 ? (
                    <div className={styles.paramList}>
                      {paramItems.map((param, i) => {
                        // 参数化主机模式下，系统自动添加的 host 参数不允许删除/改名/改类型
                        const isManagedHostParam =
                          hostMode === 'param' &&
                          executionMode === 'SSH' &&
                          param.name === 'host' &&
                          param.description === 'SSH 目标主机地址';
                        return (
                        <Card
                          key={i}
                          size="small"
                          className={styles.paramCard}
                          extra={
                            isManagedHostParam ? (
                              <Tooltip title="参数化主机模式下，host 参数由系统管理，不可删除">
                                <Tag color="blue" style={{ margin: 0 }}>系统参数</Tag>
                              </Tooltip>
                            ) : (
                              <Tooltip title="删除参数">
                                <Button
                                  type="text"
                                  size="small"
                                  danger
                                  icon={<CloseOutlined />}
                                  onClick={() => handleParamRemove(i)}
                                />
                              </Tooltip>
                            )
                          }
                          title={
                            isManagedHostParam ? (
                              <Space size={4}>
                                <Text strong style={{ fontSize: 13 }}>host</Text>
                                <Text type="secondary" style={{ fontSize: 12 }}>(SSH 目标主机地址)</Text>
                              </Space>
                            ) : (
                              <Input
                                size="small"
                                placeholder="参数名称"
                                value={param.name}
                                onChange={(e) =>
                                  handleParamItemChange(i, 'name', e.target.value)
                                }
                                style={{ width: 150 }}
                              />
                            )
                          }
                        >
                          <div className={styles.paramForm}>
                            <div className={styles.paramRow}>
                              <Text type="secondary" className={styles.paramLabel}>
                                描述
                              </Text>
                              <Input
                                size="small"
                                placeholder="参数描述"
                                value={param.description}
                                onChange={(e) =>
                                  handleParamItemChange(i, 'description', e.target.value)
                                }
                              />
                            </div>
                            <div className={styles.paramRow}>
                              <Text type="secondary" className={styles.paramLabel}>
                                类型
                              </Text>
                              <Select
                                size="small"
                                value={param.type}
                                onChange={(e) => handleParamItemChange(i, 'type', e)}
                                style={{ width: '100%' }}
                                disabled={isManagedHostParam}
                                options={[
                                  { label: '字符串', value: 'string' },
                                  { label: '整数', value: 'int' },
                                  { label: '布尔', value: 'bool' },
                                ]}
                              />
                            </div>
                            <div className={styles.paramRow}>
                              <Text type="secondary" className={styles.paramLabel}>
                                默认值
                              </Text>
                              {param.type === 'bool' ? (
                                <Radio.Group
                                  size="small"
                                  value={param.defaultValue ? 'true' : 'false'}
                                  onChange={(e) =>
                                    handleParamItemChange(
                                      i,
                                      'defaultValue',
                                      e.target.value === 'true',
                                    )
                                  }
                                >
                                  <Radio value="true">是</Radio>
                                  <Radio value="false">否</Radio>
                                </Radio.Group>
                              ) : (
                                <Input
                                  size="small"
                                  placeholder="默认值"
                                  value={param.defaultValue as string}
                                  onChange={(e) =>
                                    handleParamItemChange(i, 'defaultValue', e.target.value)
                                  }
                                />
                              )}
                            </div>
                            <div className={styles.paramRow}>
                              <Text type="secondary" className={styles.paramLabel}>
                                必填
                              </Text>
                              <Checkbox
                                checked={param.required}
                                onChange={(e) =>
                                  handleParamItemChange(i, 'required', e.target.checked)
                                }
                              >
                                是
                              </Checkbox>
                            </div>
                          </div>
                        </Card>
                        );
                      })}
                    </div>
                  ) : (
                    <Empty
                      description="暂无参数配置"
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                  )}
                  <Button
                    type="dashed"
                    block
                    icon={<PlusOutlined />}
                    onClick={handleParamAdd}
                    style={{ marginTop: 12 }}
                  >
                    添加参数
                  </Button>
                </div>
              ),
            },
            {
              key: 'env',
              label: (
                <Space>
                  <CodeOutlined />
                  <span>环境变量</span>
                  {envItems.length > 0 && <Tag>{envItems.length}</Tag>}
                </Space>
              ),
              children: (
                <div className={styles.envSection}>
                  {envItems.length > 0 ? (
                    <div className={styles.envList}>
                      {envItems.map((env, i) => (
                        <div
                          key={i}
                          className={styles.envItem}
                        >
                          <Input
                            size="small"
                            placeholder="变量名"
                            value={env.name}
                            onChange={(e) => handleEnvChange(i, 'name', e.target.value)}
                            style={{ width: 180 }}
                          />
                          <Text type="secondary">=</Text>
                          <Input
                            size="small"
                            placeholder="变量值"
                            value={env.value}
                            onChange={(e) => handleEnvChange(i, 'value', e.target.value)}
                            style={{ flex: 1 }}
                          />
                          <Button
                            type="text"
                            size="small"
                            danger
                            icon={<CloseOutlined />}
                            onClick={() => handleEnvRemove(i)}
                          />
                        </div>
                      ))}
                    </div>
                  ) : (
                    <Empty
                      description="暂无环境变量配置"
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                  )}
                  <Button
                    type="dashed"
                    block
                    icon={<PlusOutlined />}
                    onClick={handleEnvAdd}
                    style={{ marginTop: 12 }}
                  >
                    添加环境变量
                  </Button>
                </div>
              ),
            },
          ]}
        />
      </div>
    </Modal>
  );
};

export default CommandEditor;
