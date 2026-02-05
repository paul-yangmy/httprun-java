import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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
  Switch,
  Tooltip,
} from 'antd';
import FormItem from 'antd/es/form/FormItem';
import {
  PlayCircleOutlined,
  CodeOutlined,
  SettingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ThunderboltOutlined,
  StopOutlined,
  SyncOutlined,
  ExclamationCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons';
import { runCommand } from '@/services/httprun';
import { CommandStreamClient } from '@/utils/commandStream';
import styles from './index.module.less';

const { Text } = Typography;
const { confirm } = Modal;

export interface CommandExecutorProps {
  open: boolean;
  command: HTTPRUN.CommandItem;
  onClose: () => void;
}

/** 流式输出行 */
interface StreamLine {
  type: 'stdout' | 'stderr';
  content: string;
}

/** 获取命令配置，兼容旧版 command 字段 */
const getCommandConfig = (cmd: HTTPRUN.CommandItem | null | undefined): HTTPRUN.CommandConfig => {
  if (!cmd) return { command: '', params: [], env: [] };
  return cmd.commandConfig || cmd.command || { command: '', params: [], env: [] };
};

/** 获取 Token */
const getToken = (): string => {
  return localStorage.getItem('token') || '';
};

const CommandExecutor: React.FC<CommandExecutorProps> = ({
  open,
  command,
  onClose,
}) => {
  const [form] = Form.useForm();
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<HTTPRUN.CommandOutputResponse | null>(null);
  
  // WebSocket 流式模式
  const [streamMode, setStreamMode] = useState(true);
  const [streamLines, setStreamLines] = useState<StreamLine[]>([]);
  const [streamError, setStreamError] = useState<string | null>(null);
  const [streamComplete, setStreamComplete] = useState<{ exitCode: number; duration: number } | null>(null);
  
  const streamClientRef = useRef<CommandStreamClient | null>(null);
  const outputRef = useRef<HTMLDivElement>(null);
  const linesRef = useRef<StreamLine[]>([]);

  // 自动滚动到底部
  useEffect(() => {
    if (outputRef.current) {
      outputRef.current.scrollTop = outputRef.current.scrollHeight;
    }
  }, [streamLines]);

  useEffect(() => {
    if (open) {
      setResult(null);
      setStreamLines([]);
      setStreamError(null);
      setStreamComplete(null);
      linesRef.current = [];
      form.resetFields();
    }
    return () => {
      // 关闭时断开 WebSocket
      if (streamClientRef.current) {
        streamClientRef.current.disconnect();
        streamClientRef.current = null;
      }
    };
  }, [open, form]);

  /** 流式执行 */
  const handleStreamRun = useCallback(async (params: Array<{ name: string; value: string }>) => {
    setRunning(true);
    setStreamLines([]);
    setStreamError(null);
    setStreamComplete(null);
    linesRef.current = [];

    const client = new CommandStreamClient();
    streamClientRef.current = client;

    try {
      const token = getToken();
      if (!token) {
        throw new Error('Token not found');
      }

      await client.connect(token, {
        onStart: () => {
          console.log('Stream started');
        },
        onStdout: (line) => {
          const newLine = { type: 'stdout' as const, content: line };
          linesRef.current = [...linesRef.current, newLine];
          setStreamLines([...linesRef.current]);
        },
        onStderr: (line) => {
          const newLine = { type: 'stderr' as const, content: line };
          linesRef.current = [...linesRef.current, newLine];
          setStreamLines([...linesRef.current]);
        },
        onError: (error) => {
          setStreamError(error);
        },
        onComplete: (exitCode, duration) => {
          setRunning(false);
          setStreamComplete({ exitCode, duration });

          // 执行历史由后端 AccessLog 记录，无需本地存储

          if (exitCode === 0) {
            message.success('命令执行成功');
          } else {
            message.warning(`命令退出码: ${exitCode}`);
          }
        },
        onCancelled: () => {
          setRunning(false);
          message.info('命令已取消');
        },
        onClose: () => {
          setRunning(false);
        },
      });

      client.runCommand(command.name, params);
    } catch (error: any) {
      setRunning(false);
      setStreamError(error.message || 'WebSocket 连接失败');
      message.error('WebSocket 连接失败，请尝试使用普通模式');
    }
  }, [command]);

  /** 取消流式执行 */
  const handleCancel = useCallback(() => {
    if (streamClientRef.current) {
      streamClientRef.current.cancel();
    }
  }, []);

  /** 普通执行 */
  const handleNormalRun = useCallback((params: HTTPRUN.Param[]) => {
    setRunning(true);
    setResult(null);
    
    runCommand(command.name, params, [])
      .then((r) => {
        setRunning(false);
        setResult(r);

        // 执行历史由后端 AccessLog 自动记录，无需本地存储

        if (!r.error) {
          message.success('命令执行成功');
        } else {
          message.error(r.stderr || '执行失败: ' + r.error);
        }
      })
      .catch(() => {
        setRunning(false);
        // 执行历史由后端 AccessLog 自动记录，无需本地存储
        message.error('命令执行失败');
      });
  }, [command]);

  /** 执行命令（带高危确认） */
  const executeCommand = useCallback((params: Array<{ name: string; value: any }>) => {
    if (streamMode) {
      handleStreamRun(params.map(p => ({ name: p.name, value: String(p.value ?? '') })));
    } else {
      handleNormalRun(params);
    }
  }, [streamMode, handleStreamRun, handleNormalRun]);

  /** 显示高危命令确认弹窗 */
  const showDangerConfirm = useCallback((params: Array<{ name: string; value: any }>) => {
    const dangerLevel = command.dangerLevel || 0;
    const dangerWarning = command.dangerWarning || '此命令被标记为高危操作，请确认后执行！';
    
    confirm({
      title: dangerLevel >= 2 ? '⚠️ 高危命令确认' : '⚡ 危险操作确认',
      icon: dangerLevel >= 2 ? <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} /> : <WarningOutlined style={{ color: '#faad14' }} />,
      content: (
        <div>
          <Alert 
            message={dangerWarning}
            type={dangerLevel >= 2 ? 'error' : 'warning'}
            showIcon
            style={{ marginBottom: 16 }}
          />
          <p>命令名称：<strong>{command.name}</strong></p>
          <p style={{ color: '#666', fontSize: 12 }}>
            请确认您了解此操作的影响，执行后可能无法撤销。
          </p>
        </div>
      ),
      okText: '确认执行',
      okType: dangerLevel >= 2 ? 'danger' : 'primary',
      cancelText: '取消',
      onOk() {
        executeCommand(params);
      },
      onCancel() {
        message.info('已取消执行');
      },
    });
  }, [command, executeCommand]);

  const handleRun = useCallback(() => {
    form.validateFields().then(() => {
      const values = form.getFieldsValue();
      const params: Array<{ name: string; value: any }> = [];
      Object.keys(values).forEach((key) => {
        let val = values[key];
        if (typeof val === 'boolean') {
          val = val ? 'true' : 'false';
        }
        params.push({ name: key, value: val });
      });

      // 检查是否为高危命令，需要二次确认
      const dangerLevel = command.dangerLevel || 0;
      if (dangerLevel > 0) {
        showDangerConfirm(params);
      } else {
        executeCommand(params);
      }
    }).catch(() => {
      message.warning('请填写必填参数');
    });
  }, [form, command, showDangerConfirm, executeCommand]);

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

  // 使用 useWatch 监听所有表单字段的变化
  const formValues = Form.useWatch([], form);

  // 实时生成预览命令
  const previewCommand = useMemo(() => {
    const values = formValues || {};
    let cmd = cfg.command || '';

    // 替换参数占位符
    cfg.params?.forEach((param) => {
      const placeholder = `{{.${param.name}}}`;
      let value = values[param.name];

      // 处理不同类型的值
      if (value === undefined || value === null || value === '') {
        value = param.defaultValue !== undefined ? param.defaultValue : `<${param.name}>`;
      }
      
      if (typeof value === 'boolean') {
        value = value ? 'true' : 'false';
      }

      // 替换所有出现的占位符
      cmd = cmd.replace(new RegExp(placeholder.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), String(value));
    });

    return cmd;
  }, [cfg, formValues]);

  // 渲染流式输出
  const renderStreamOutput = () => {
    if (streamLines.length === 0 && !streamError && !streamComplete && !running) {
      return null;
    }

    return (
      <>
        <Divider />
        <div className={styles.result}>
          <Space style={{ marginBottom: 12 }}>
            {running ? (
              <>
                <SyncOutlined spin style={{ color: '#1677ff' }} />
                <Text strong style={{ color: '#1677ff' }}>
                  执行中...
                </Text>
              </>
            ) : streamComplete ? (
              streamComplete.exitCode === 0 ? (
                <>
                  <CheckCircleOutlined style={{ color: '#52c41a' }} />
                  <Text strong style={{ color: '#52c41a' }}>
                    执行成功
                  </Text>
                </>
              ) : (
                <>
                  <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                  <Text strong style={{ color: '#ff4d4f' }}>
                    退出码: {streamComplete.exitCode}
                  </Text>
                </>
              )
            ) : null}
            {streamComplete && (
              <Text type="secondary">耗时: {streamComplete.duration}ms</Text>
            )}
          </Space>

          {streamError && (
            <Alert
              type="error"
              message="执行错误"
              description={streamError}
              style={{ marginBottom: 12 }}
            />
          )}

          {(streamLines.length > 0 || running) && (
            <div className={styles.streamOutput} ref={outputRef}>
              {streamLines.map((line, index) => (
                <pre
                  key={index}
                  className={`${styles.streamLine} ${
                    line.type === 'stdout' ? styles.stdoutLine : styles.stderrLine
                  }`}
                >
                  {line.content}
                </pre>
              ))}
              {running && streamLines.length === 0 && (
                <Text type="secondary" style={{ fontStyle: 'italic' }}>
                  等待输出...
                </Text>
              )}
            </div>
          )}
        </div>
      </>
    );
  };

  // 渲染普通输出
  const renderNormalOutput = () => {
    if (!result) return null;

    return (
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
    );
  };

  return (
    <Modal
      open={open}
      title={
        <Space>
          <PlayCircleOutlined style={{ color: '#1677ff' }} />
          <span>运行命令</span>
          <Tag color="blue">{command.name}</Tag>
          {/* 高危命令标识 */}
          {command.dangerLevel && command.dangerLevel >= 2 && (
            <Tag color="error" icon={<ExclamationCircleOutlined />}>高危</Tag>
          )}
          {command.dangerLevel === 1 && (
            <Tag color="warning" icon={<WarningOutlined />}>警告</Tag>
          )}
        </Space>
      }
      onCancel={onClose}
      width={700}
      footer={[
        <Button key="close" onClick={onClose}>
          关闭
        </Button>,
        running && streamMode ? (
          <Button
            key="cancel"
            danger
            icon={<StopOutlined />}
            onClick={handleCancel}
          >
            取消执行
          </Button>
        ) : (
          <Button
            key="run"
            type="primary"
            danger={command.dangerLevel && command.dangerLevel >= 2}
            icon={<PlayCircleOutlined />}
            onClick={handleRun}
            loading={running}
          >
            {running ? '执行中...' : '运行命令'}
          </Button>
        ),
      ]}
    >
      <div className={styles.executor}>
        {/* 高危命令警告 */}
        {command.dangerLevel && command.dangerLevel > 0 && (
          <Alert
            message={command.dangerLevel >= 2 ? '⚠️ 高危命令' : '⚡ 危险操作'}
            description={command.dangerWarning || '此命令被标记为危险操作，执行前请确认！'}
            type={command.dangerLevel >= 2 ? 'error' : 'warning'}
            showIcon
            icon={command.dangerLevel >= 2 ? <ExclamationCircleOutlined /> : <WarningOutlined />}
            style={{ marginBottom: 16 }}
          />
        )}

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

        {/* 命令预览 */}
        {cfg.command && (
          <Alert
            message={
              <Space direction="vertical" style={{ width: '100%' }} size="small">
                <div>
                  <Text strong>实际执行命令预览</Text>
                  <Tooltip title="随着参数配置自动更新">
                    <Text type="secondary" style={{ marginLeft: 8, fontSize: '12px' }}>
                      (实时预览)
                    </Text>
                  </Tooltip>
                </div>
                <div className={styles.commandPreview}>
                  <pre className={styles.commandPreviewCode}>
                    <code>{previewCommand}</code>
                  </pre>
                </div>
              </Space>
            }
            type="info"
            showIcon
            icon={<CodeOutlined />}
            style={{ marginBottom: 16 }}
          />
        )}

        {/* 执行模式切换 */}
        <div className={styles.streamMode}>
          <Space>
            <Tooltip title="实时模式通过 WebSocket 流式获取输出，适合长时间运行的命令；普通模式等待命令完成后返回全部输出">
              <ThunderboltOutlined style={{ color: streamMode ? '#1677ff' : undefined }} />
            </Tooltip>
            <Text>实时输出模式</Text>
            <Switch
              checked={streamMode}
              onChange={setStreamMode}
              disabled={running}
              size="small"
            />
          </Space>
        </div>

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
                <Form 
                  form={form} 
                  initialValues={initialValues} 
                  layout="vertical"
                >
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
        {streamMode ? renderStreamOutput() : renderNormalOutput()}
      </div>
    </Modal>
  );
};

export default CommandExecutor;
