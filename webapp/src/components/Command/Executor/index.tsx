import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Checkbox,
  Form,
  Input,
  Modal,
  Typography,
  message,
  Space,
  Tag,
  Card,
  Divider,
  Button,
  Tabs,
  Empty,
} from 'antd';
import FormItem from 'antd/es/form/FormItem';
import {
  PlayCircleOutlined,
  CodeOutlined,
  SettingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import { runCommand } from '@/services/httprun';
import styles from './index.module.less';

const { Text } = Typography;

export interface CommandExecutorProps {
  open: boolean;
  command: HTTPRUN.CommandItem;
  onClose: () => void;
}

/** 获取命令配置，兼容旧版 command 字段 */
const getCommandConfig = (cmd: HTTPRUN.CommandItem | null | undefined): HTTPRUN.CommandConfig => {
  if (!cmd) return { command: '', params: [], env: [] };
  return cmd.commandConfig || cmd.command || { command: '', params: [], env: [] };
};

const CommandExecutor: React.FC<CommandExecutorProps> = ({
  open,
  command,
  onClose,
}) => {
  const [form] = Form.useForm();
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<HTTPRUN.CommandOutputResponse | null>(null);

  useEffect(() => {
    if (open) {
      setResult(null);
      form.resetFields();
    }
  }, [open, form]);

  const handleRun = useCallback(() => {
    form
      .validateFields()
      .then(() => {
        setRunning(true);
        setResult(null);
        const values = form.getFieldsValue();
        const params: HTTPRUN.Param[] = [];
        Object.keys(values).forEach((key) => {
          let val = values[key];
          if (['true', 'false'].includes(val)) {
            val = val === 'true';
          }
          params.push({ name: key, value: val });
        });
        runCommand(command.name, params, [])
          .then((r) => {
            setRunning(false);
            setResult(r);

            if (!r.error) {
              message.success('命令执行成功');
            } else {
              message.error(r.stderr || '执行失败: ' + r.error);
            }
          })
          .catch(() => {
            setRunning(false);
            message.error('命令执行失败');
          });
      })
      .catch(() => {
        message.warning('请填写必填参数');
      });
  }, [command, form]);

  const initialValues = useMemo(() => {
    const val: Record<string, string | number | boolean | undefined> = {};
    const cfg = getCommandConfig(command);
    cfg.params?.forEach((p) => {
      val[p.name] = p.defaultValue;
    });
    return val;
  }, [command]);

  const cfg = getCommandConfig(command);
  const hasParams = cfg.params && cfg.params.length > 0;
  const hasEnv = cfg.env && cfg.env.length > 0;

  return (
    <Modal
      open={open}
      title={
        <Space>
          <PlayCircleOutlined style={{ color: '#1677ff' }} />
          <span>运行命令</span>
          <Tag color="blue">{command.name}</Tag>
        </Space>
      }
      onCancel={onClose}
      width={700}
      footer={[
        <Button key="close" onClick={onClose}>
          关闭
        </Button>,
        <Button
          key="run"
          type="primary"
          icon={<PlayCircleOutlined />}
          onClick={handleRun}
          loading={running}
        >
          {running ? '执行中...' : '运行命令'}
        </Button>,
      ]}
    >
      <div className={styles.executor}>
        {/* 命令描述 */}
        <Card size="small" className={styles.descCard}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <Text type="secondary">{command.description || '暂无描述'}</Text>
            <div>
              <Text type="secondary">命令路径：</Text>
              <Text code copyable>
                {command.path}
              </Text>
            </div>
          </Space>
        </Card>

        <Tabs
          defaultActiveKey="params"
          items={[
            {
              key: 'params',
              label: (
                <Space>
                  <SettingOutlined />
                  <span>参数配置</span>
                  {hasParams && <Tag>{cfg.params.length}</Tag>}
                </Space>
              ),
              children: (
                <Form form={form} initialValues={initialValues} layout="vertical">
                  {hasParams ? (
                    <div className={styles.paramList}>
                      {cfg.params.map((item) => (
                        <Card key={item.name} size="small" className={styles.paramCard}>
                          <div className={styles.paramHeader}>
                            <Space>
                              {item.required && (
                                <span className={styles.required}>*</span>
                              )}
                              <Text strong>{item.name}</Text>
                              <Tag
                                color={
                                  item.type === 'bool'
                                    ? 'purple'
                                    : item.type === 'int'
                                    ? 'orange'
                                    : 'blue'
                                }
                              >
                                {item.type}
                              </Tag>
                            </Space>
                          </div>
                          {item.description && (
                            <Text type="secondary" className={styles.paramDesc}>
                              {item.description}
                            </Text>
                          )}
                          <FormItem
                            name={item.name}
                            rules={[
                              { required: item.required, message: `请输入 ${item.name}` },
                            ]}
                            valuePropName={item.type === 'bool' ? 'checked' : 'value'}
                            style={{ marginBottom: 0, marginTop: 8 }}
                          >
                            {item.type === 'bool' ? (
                              <Checkbox>启用</Checkbox>
                            ) : (
                              <Input
                                placeholder={`请输入 ${item.name}`}
                                type={item.type === 'int' ? 'number' : 'text'}
                              />
                            )}
                          </FormItem>
                        </Card>
                      ))}
                    </div>
                  ) : (
                    <Empty
                      description="此命令无需参数"
                      image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                  )}
                </Form>
              ),
            },
            {
              key: 'env',
              label: (
                <Space>
                  <CodeOutlined />
                  <span>环境变量</span>
                  {hasEnv && <Tag>{cfg.env.length}</Tag>}
                </Space>
              ),
              children: hasEnv ? (
                <div className={styles.envList}>
                  {cfg.env.map((env) => (
                    <div key={env.name} className={styles.envItem}>
                      <Text code>{env.name}</Text>
                      <Text type="secondary">=</Text>
                      <Text>{env.value}</Text>
                    </div>
                  ))}
                </div>
              ) : (
                <Empty
                  description="无环境变量配置"
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                />
              ),
            },
          ]}
        />

        {/* 执行结果 */}
        {result && (
          <>
            <Divider />
            <div className={styles.result}>
              <Space style={{ marginBottom: 12 }}>
                {result.error ? (
                  <>
                    <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                    <Text strong style={{ color: '#ff4d4f' }}>
                      执行失败
                    </Text>
                  </>
                ) : (
                  <>
                    <CheckCircleOutlined style={{ color: '#52c41a' }} />
                    <Text strong style={{ color: '#52c41a' }}>
                      执行成功
                    </Text>
                  </>
                )}
              </Space>
              {result.error && (
                <Alert
                  type="error"
                  message={result.error}
                  description={result.stderr}
                  style={{ marginBottom: 12 }}
                />
              )}
              {result.stdout && (
                <div className={styles.output}>
                  <Text type="secondary" style={{ marginBottom: 8, display: 'block' }}>
                    输出内容
                  </Text>
                  <pre className={styles.outputContent}>
                    {result.stdout}
                  </pre>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </Modal>
  );
};

export default CommandExecutor;
