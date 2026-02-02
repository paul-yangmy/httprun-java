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
} from '@ant-design/icons';
import { runCommand } from '@/services/httprun';
import { CommandStreamClient } from '@/utils/commandStream';
import styles from './index.module.less';

const { Text } = Typography;

export interface CommandExecutorProps {
  open: boolean;
  command: HTTPRUN.CommandItem;
  onClose: () => void;
}

/** 执行历史项 */
interface ExecutionHistoryItem {
  id: number;
  commandName: string;
  params: string;
  status: 'success' | 'error' | 'running';
  stdout: string;
  stderr: string;
  duration: number;
  executedAt: string;
  ip: string;
}

/** 流式输出行 */
interface StreamLine {
  type: 'stdout' | 'stderr';
  content: string;
}

/** 保存执行历史到 localStorage */
const saveExecutionHistory = (item: ExecutionHistoryItem) => {
  try {
    const historyStr = localStorage.getItem('command_execution_history') || '[]';
    const history: ExecutionHistoryItem[] = JSON.parse(historyStr);
    // 限制最多保存 200 条记录
    if (history.length >= 200) {
      history.shift();
    }
    history.push(item);
    localStorage.setItem('command_execution_history', JSON.stringify(history));
  } catch (e) {
    console.error('Failed to save execution history:', e);
  }
};

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

          // 收集所有输出保存历史
          const allStdout = linesRef.current
            .filter((l) => l.type === 'stdout')
            .map((l) => l.content)
            .join('\n');
          const allStderr = linesRef.current
            .filter((l) => l.type === 'stderr')
            .map((l) => l.content)
            .join('\n');

          const historyItem: ExecutionHistoryItem = {
            id: Date.now(),
            commandName: command.name,
            params: JSON.stringify(params),
            status: exitCode === 0 ? 'success' : 'error',
            stdout: allStdout,
            stderr: allStderr,
            duration,
            executedAt: new Date().toISOString(),
            ip: '-',
          };
          saveExecutionHistory(historyItem);

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
    
    const startTime = Date.now();
    
    runCommand(command.name, params, [])
      .then((r) => {
        const duration = Date.now() - startTime;
        setRunning(false);
        setResult(r);

        // 保存执行历史
        const historyItem: ExecutionHistoryItem = {
          id: Date.now(),
          commandName: command.name,
          params: JSON.stringify(params),
          status: r.error ? 'error' : 'success',
          stdout: r.stdout || '',
          stderr: r.stderr || '',
          duration,
          executedAt: new Date().toISOString(),
          ip: '-',
        };
        saveExecutionHistory(historyItem);

        if (!r.error) {
          message.success('命令执行成功');
        } else {
          message.error(r.stderr || '执行失败: ' + r.error);
        }
      })
      .catch(() => {
        const duration = Date.now() - startTime;
        setRunning(false);
        
        // 保存失败的执行历史
        const historyItem: ExecutionHistoryItem = {
          id: Date.now(),
          commandName: command.name,
          params: JSON.stringify(params),
          status: 'error',
          stdout: '',
          stderr: '命令执行失败',
          duration,
          executedAt: new Date().toISOString(),
          ip: '-',
        };
        saveExecutionHistory(historyItem);
        
        message.error('命令执行失败');
      });
  }, [command]);

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

      if (streamMode) {
        handleStreamRun(params.map(p => ({ name: p.name, value: String(p.value ?? '') })));
      } else {
        handleNormalRun(params);
      }
    }).catch(() => {
      message.warning('请填写必填参数');
    });
  }, [form, streamMode, handleStreamRun, handleNormalRun]);

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
        {streamMode ? renderStreamOutput() : renderNormalOutput()}
      </div>
    </Modal>
  );
};

export default CommandExecutor;
