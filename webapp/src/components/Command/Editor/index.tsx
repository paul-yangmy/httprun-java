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
} from 'antd';
import {
  CloseOutlined,
  PlusOutlined,
  CodeOutlined,
  SettingOutlined,
  EditOutlined,
} from '@ant-design/icons';
import { createCommand, updateCommand } from '@/services/httprun';
import styles from './index.module.less';

const { Text } = Typography;

export interface CommandEditorProps {
  open: boolean;
  command: HTTPRUN.CommandItem | Record<string, never>;
  onClose: () => void;
  onChange?: () => void;
}

/** 获取命令配置，兼容旧版 command 字段 */
const getCommandConfig = (cmd: HTTPRUN.CommandItem | null | undefined): HTTPRUN.CommandConfig => {
  if (!cmd) return { command: '', params: [], env: [] };
  return cmd.commandConfig || cmd.command || { command: '', params: [], env: [] };
};

const CommandEditor: React.FC<CommandEditorProps> = ({
  open,
  onClose,
  command,
  onChange,
}) => {
  const [value, setValue] = useState<HTTPRUN.CommandItem>(
    (command as HTTPRUN.CommandItem) || { commandConfig: { command: '', params: [], env: [] } },
  );
  const [form] = Form.useForm();
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    setValue(
      (command as HTTPRUN.CommandItem) || { commandConfig: { command: '', params: [], env: [] } },
    );
    if (command && 'name' in command) {
      const cfg = getCommandConfig(command as HTTPRUN.CommandItem);
      form.setFieldsValue({
        name: command.name,
        description: command.description,
        command: cfg.command,
      });
    } else {
      form.resetFields();
    }
  }, [command, form]);

  const isEdit = command && 'name' in command && command.name;
  const title = isEdit ? '编辑命令' : '添加命令';

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
        
        // 构建请求对象，使用 commandConfig 字段
        const req: any = {
          name: val.name,
          description: val.description,
          commandConfig: cfg,
          status: 0,
          path: `/api/run/${val.name}`,
        };
        
        createCommand(req)
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
  }, [form, value, onChange, onClose, isEdit]);

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
              rules={[{ required: true, message: '请输入命令名称' }]}
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
          <Form.Item
            label="命令内容"
            name="command"
            rules={[{ required: true, message: '请输入命令内容' }]}
            tooltip="要执行的 Shell 命令，支持参数替换"
          >
            <Input.TextArea
              rows={4}
              placeholder="如：docker deploy {{.app_name}} --env {{.env}}"
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
                      {paramItems.map((param, i) => (
                        <Card
                          key={i}
                          size="small"
                          className={styles.paramCard}
                          extra={
                            <Tooltip title="删除参数">
                              <Button
                                type="text"
                                size="small"
                                danger
                                icon={<CloseOutlined />}
                                onClick={() => handleParamRemove(i)}
                              />
                            </Tooltip>
                          }
                          title={
                            <Input
                              size="small"
                              placeholder="参数名称"
                              value={param.name}
                              onChange={(e) =>
                                handleParamItemChange(i, 'name', e.target.value)
                              }
                              style={{ width: 150 }}
                            />
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
                      ))}
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
