import React, { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Table,
  Tag,
  Space,
  Input,
  Typography,
  Tooltip,
  Empty,
  Modal,
  Descriptions,
  message,
  Select,
  DatePicker,
  Badge,
  Popconfirm,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  HistoryOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  PlayCircleOutlined,
  DeleteOutlined,
  ClearOutlined,
  UserOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer } from '@ant-design/pro-components';
import dayjs from 'dayjs';
import {
  getUserCommandList,
  runCommand,
  getExecutionHistory,
  deleteExecutionHistoryItem,
  deleteExecutionHistoryBatch,
  clearExecutionHistory,
  getCurrentUser,
  getTokenList,
} from '@/services/httprun';

const { Text } = Typography;
const { RangePicker } = DatePicker;

/**
 * 格式化 JSON 字符串
 */
const formatJson = (str: string | null | undefined): string => {
  if (!str) return '';
  try {
    const obj = JSON.parse(str);
    return JSON.stringify(obj, null, 2);
  } catch {
    return str;
  }
};

const CommandHistory: React.FC = () => {
  const [data, setData] = useState<HTTPRUN.ExecutionHistoryResponse>();
  const [loading, setLoading] = useState<boolean>(false);
  const [searchText, setSearchText] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs | null, dayjs.Dayjs | null]>([null, null]);
  const [detailModal, setDetailModal] = useState<HTTPRUN.AccessLogItem | null>(null);
  const [commandList, setCommandList] = useState<HTTPRUN.CommandItem[]>([]);
  const [commandFilter, setCommandFilter] = useState<string>('');
  const [page, setPage] = useState({ page: 1, pageSize: 20 });
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [isAdmin, setIsAdmin] = useState(false);
  const [tokenList, setTokenList] = useState<HTTPRUN.Token[]>([]);
  const [tokenFilter, setTokenFilter] = useState<string>('');

  // 检查用户权限
  useEffect(() => {
    getCurrentUser()
      .then((user) => {
        setIsAdmin(user?.isAdmin || false);
        if (user?.isAdmin) {
          // 管理员加载 Token 列表用于筛选
          getTokenList()
            .then((list) => setTokenList(list || []))
            .catch(() => {});
        }
      })
      .catch(() => {});
  }, []);

  // 从 API 获取执行历史
  const loadHistory = useCallback(() => {
    setLoading(true);
    
    const params: {
      page: number;
      pageSize: number;
      tokenName?: string;
      commandName?: string;
      status?: string;
      startTime?: string;
      endTime?: string;
      keyword?: string;
    } = {
      page: page.page,
      pageSize: page.pageSize,
    };
    
    // 筛选条件
    if (tokenFilter) params.tokenName = tokenFilter;
    if (commandFilter) params.commandName = commandFilter;
    if (statusFilter) params.status = statusFilter;
    if (searchText) params.keyword = searchText;
    if (dateRange[0] && dateRange[1]) {
      params.startTime = dateRange[0].startOf('day').format('YYYY-MM-DDTHH:mm:ss');
      params.endTime = dateRange[1].endOf('day').format('YYYY-MM-DDTHH:mm:ss');
    }
    
    getExecutionHistory(params)
      .then((response) => {
        setData(response);
        setLoading(false);
        setSelectedRowKeys([]);
      })
      .catch(() => {
        message.error('获取执行历史失败');
        setLoading(false);
      });
  }, [page, tokenFilter, commandFilter, statusFilter, searchText, dateRange]);

  // 获取命令列表用于筛选
  const loadCommands = useCallback(() => {
    getUserCommandList()
      .then((list) => setCommandList(list || []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    loadHistory();
    loadCommands();
  }, [loadHistory, loadCommands]);

  // 删除单条记录
  const handleDelete = (id: number) => {
    deleteExecutionHistoryItem(id)
      .then((res) => {
        if (res.success) {
          message.success('删除成功');
          loadHistory();
        } else {
          message.error(res.message || '删除失败');
        }
      })
      .catch(() => message.error('删除失败'));
  };

  // 批量删除
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的记录');
      return;
    }
    
    deleteExecutionHistoryBatch(selectedRowKeys.map(Number))
      .then((res) => {
        if (res.success) {
          message.success(res.message || '删除成功');
          loadHistory();
        } else {
          message.error(res.message || '删除失败');
        }
      })
      .catch(() => message.error('删除失败'));
  };

  // 清空所有记录
  const handleClear = () => {
    clearExecutionHistory()
      .then((res) => {
        if (res.success) {
          message.success(res.message || '清空成功');
          loadHistory();
        } else {
          message.error(res.message || '清空失败');
        }
      })
      .catch(() => message.error('清空失败'));
  };

  // 重新执行命令
  const handleRerun = (item: HTTPRUN.AccessLogItem) => {
    if (!item.commandName) {
      message.warning('无法获取命令名称');
      return;
    }
    
    const command = commandList.find((c) => c.name === item.commandName);
    if (!command) {
      message.warning('命令不存在或您没有执行权限');
      return;
    }
    
    // 解析参数
    let params: HTTPRUN.Param[] = [];
    try {
      const requestData = JSON.parse(item.request || '{}');
      params = requestData.params || [];
    } catch {
      params = [];
    }
    
    message.loading({ content: '正在执行...', key: 'rerun' });
    
    runCommand(item.commandName, params, [])
      .then(() => {
        message.success({ content: '执行完成', key: 'rerun' });
        // 重新加载历史记录
        loadHistory();
      })
      .catch(() => {
        message.error({ content: '执行失败', key: 'rerun' });
      });
  };

  // 重置筛选条件
  const handleReset = () => {
    setSearchText('');
    setCommandFilter('');
    setStatusFilter('');
    setTokenFilter('');
    setDateRange([null, null]);
    setPage({ page: 1, pageSize: 20 });
  };

  const getStatusTag = (statusCode: number) => {
    if (statusCode >= 200 && statusCode < 300) {
      return <Tag icon={<CheckCircleOutlined />} color="success">成功</Tag>;
    } else if (statusCode >= 400) {
      return <Tag icon={<CloseCircleOutlined />} color="error">失败</Tag>;
    } else {
      return <Tag icon={<ClockCircleOutlined />} color="processing">{statusCode}</Tag>;
    }
  };

  const columns: ColumnsType<HTTPRUN.AccessLogItem> = [
    {
      title: '命令名称',
      dataIndex: 'commandName',
      key: 'commandName',
      width: 150,
      render: (name: string) => (
        <Text strong style={{ color: '#1677ff' }}>
          {name || '-'}
        </Text>
      ),
    },
    // 管理员显示 Token 列
    ...(isAdmin
      ? [
          {
            title: '执行用户',
            dataIndex: 'tokenId',
            key: 'tokenId',
            width: 120,
            render: (tokenId: string) => (
              <Tag icon={<UserOutlined />} color="blue">
                {tokenId}
              </Tag>
            ),
          },
        ]
      : []),
    {
      title: '请求路径',
      dataIndex: 'path',
      key: 'path',
      width: 180,
      ellipsis: true,
      render: (path: string) => (
        <Tooltip title={path}>
          <Text code style={{ fontSize: 12 }}>{path}</Text>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'statusCode',
      key: 'statusCode',
      width: 90,
      align: 'center',
      render: (statusCode: number) => getStatusTag(statusCode),
    },
    {
      title: '耗时',
      dataIndex: 'duration',
      key: 'duration',
      width: 80,
      align: 'right',
      render: (duration: number) => (
        <Text type={duration > 5000 ? 'danger' : 'secondary'}>
          {duration > 0 ? `${duration}ms` : '-'}
        </Text>
      ),
    },
    {
      title: '来源',
      dataIndex: 'source',
      key: 'source',
      width: 70,
      align: 'center',
      render: (source: string) => (
        <Tag color={source === 'WEB' ? 'blue' : source === 'CLI' ? 'green' : 'default'}>
          {source || '-'}
        </Tag>
      ),
    },
    {
      title: 'IP',
      dataIndex: 'ip',
      key: 'ip',
      width: 120,
      render: (ip: string) => <Text type="secondary" style={{ fontSize: 12 }}>{ip}</Text>,
    },
    {
      title: '执行时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (time: string) => (
        <Text type="secondary" style={{ fontSize: 12 }}>
          {dayjs(time).format('YYYY-MM-DD HH:mm:ss')}
        </Text>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      align: 'center',
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="查看详情">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => setDetailModal(record)}
            />
          </Tooltip>
          {record.commandName && (
            <Tooltip title="重新执行">
              <Button
                type="link"
                size="small"
                icon={<PlayCircleOutlined />}
                onClick={() => handleRerun(record)}
              />
            </Tooltip>
          )}
          <Popconfirm
            title="确定删除此记录？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Tooltip title="删除">
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
              />
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer>
      <Card
        title={
          <Space>
            <HistoryOutlined style={{ color: '#1677ff' }} />
            <span>执行历史</span>
            <Badge count={data?.totalElements || 0} showZero color="#1677ff" />
            {isAdmin && <Tag color="gold">管理员模式</Tag>}
          </Space>
        }
        extra={
          <Space wrap>
            <Input
              placeholder="搜索命令或路径"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              onPressEnter={loadHistory}
              style={{ width: 150 }}
              allowClear
            />
            {isAdmin && (
              <Select
                placeholder="用户筛选"
                value={tokenFilter || undefined}
                onChange={(v) => { setTokenFilter(v || ''); setPage({ ...page, page: 1 }); }}
                style={{ width: 120 }}
                allowClear
              >
                {tokenList.map((token) => (
                  <Select.Option key={token.name} value={token.name}>
                    {token.name}
                  </Select.Option>
                ))}
              </Select>
            )}
            <Select
              placeholder="命令筛选"
              value={commandFilter || undefined}
              onChange={(v) => { setCommandFilter(v || ''); setPage({ ...page, page: 1 }); }}
              style={{ width: 120 }}
              allowClear
            >
              {commandList.map((cmd) => (
                <Select.Option key={cmd.name} value={cmd.name}>
                  {cmd.name}
                </Select.Option>
              ))}
            </Select>
            <Select
              placeholder="状态"
              value={statusFilter || undefined}
              onChange={(v) => { setStatusFilter(v || ''); setPage({ ...page, page: 1 }); }}
              style={{ width: 90 }}
              allowClear
            >
              <Select.Option value="success">成功</Select.Option>
              <Select.Option value="error">失败</Select.Option>
            </Select>
            <RangePicker
              value={dateRange}
              onChange={(dates) => {
                setDateRange(dates as [dayjs.Dayjs | null, dayjs.Dayjs | null]);
                setPage({ ...page, page: 1 });
              }}
              style={{ width: 220 }}
            />
            <Button onClick={handleReset}>重置</Button>
            <Tooltip title="重新加载数据">
              <Button icon={<ReloadOutlined />} onClick={() => {
                setData(undefined);
                loadHistory();
                message.success('已刷新数据');
              }}>
                刷新
              </Button>
            </Tooltip>
          </Space>
        }
        bordered={false}
      >
        {/* 批量操作栏 */}
        {selectedRowKeys.length > 0 && (
          <Space style={{ marginBottom: 16 }}>
            <Text>已选择 {selectedRowKeys.length} 项</Text>
            <Popconfirm
              title={`确定删除选中的 ${selectedRowKeys.length} 条记录？`}
              onConfirm={handleBatchDelete}
              okText="确定"
              cancelText="取消"
            >
              <Button danger icon={<DeleteOutlined />}>
                批量删除
              </Button>
            </Popconfirm>
            <Button onClick={() => setSelectedRowKeys([])}>取消选择</Button>
          </Space>
        )}
        
        {/* 清空按钮 */}
        {(data?.totalElements ?? 0) > 0 && selectedRowKeys.length === 0 && (
          <div style={{ marginBottom: 16 }}>
            <Popconfirm
              title="确定清空您的所有执行记录？此操作不可恢复！"
              onConfirm={handleClear}
              okText="确定清空"
              cancelText="取消"
              okButtonProps={{ danger: true }}
            >
              <Button danger icon={<ClearOutlined />}>
                清空我的记录
              </Button>
            </Popconfirm>
          </div>
        )}
        
        <Table
          columns={columns}
          dataSource={data?.content}
          rowKey="id"
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          pagination={{
            current: page.page,
            pageSize: page.pageSize,
            total: data?.totalElements,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
            onChange: (newPage, pageSize) => setPage({ page: newPage, pageSize }),
          }}
          scroll={{ x: 1100 }}
          locale={{
            emptyText: (
              <Empty description="暂无执行历史" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                <Text type="secondary">执行命令后会自动记录</Text>
              </Empty>
            ),
          }}
        />
      </Card>

      {/* 详情弹窗 */}
      <Modal
        title={
          <Space>
            <HistoryOutlined />
            <span>执行详情</span>
          </Space>
        }
        open={!!detailModal}
        onCancel={() => setDetailModal(null)}
        footer={[
          detailModal?.commandName && (
            <Button key="rerun" type="primary" icon={<PlayCircleOutlined />} onClick={() => {
              if (detailModal) handleRerun(detailModal);
              setDetailModal(null);
            }}>
              重新执行
            </Button>
          ),
          <Popconfirm
            key="delete"
            title="确定删除此记录？"
            onConfirm={() => {
              if (detailModal) {
                handleDelete(detailModal.id);
                setDetailModal(null);
              }
            }}
            okText="确定"
            cancelText="取消"
          >
            <Button danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>,
          <Button key="close" onClick={() => setDetailModal(null)}>
            关闭
          </Button>,
        ].filter(Boolean)}
        width={750}
      >
        {detailModal && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="命令名称" span={1}>
              <Text strong>{detailModal.commandName || '-'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="执行状态" span={1}>
              {getStatusTag(detailModal.statusCode)}
            </Descriptions.Item>
            <Descriptions.Item label="执行用户" span={1}>
              <Tag icon={<UserOutlined />} color="blue">{detailModal.tokenId}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="来源" span={1}>
              <Tag>{detailModal.source || '-'}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="请求路径" span={2}>
              <Text code copyable>{detailModal.path}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="执行时间" span={1}>
              {dayjs(detailModal.createdAt).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
            <Descriptions.Item label="耗时" span={1}>
              {detailModal.duration > 0 ? `${detailModal.duration}ms` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="客户端 IP" span={1}>
              <Tag color="blue">{detailModal.ip}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="请求 ID" span={1}>
              <Text code style={{ fontSize: 11 }}>{detailModal.requestId || '-'}</Text>
            </Descriptions.Item>
            {detailModal.userAgent && (
              <Descriptions.Item label="User-Agent" span={2}>
                <Text type="secondary" style={{ fontSize: 11 }}>{detailModal.userAgent}</Text>
              </Descriptions.Item>
            )}
            <Descriptions.Item label="请求内容" span={2}>
              <pre
                style={{
                  marginBottom: 0,
                  maxHeight: 200,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                  fontSize: 12,
                  background: '#f6f8fa',
                  padding: 12,
                  borderRadius: 6,
                  border: '1px solid #d0d7de',
                  fontFamily: 'Monaco, Menlo, Consolas, monospace',
                }}
              >
                {formatJson(detailModal.request) || '(无请求内容)'}
              </pre>
            </Descriptions.Item>
            <Descriptions.Item label="响应内容" span={2}>
              <pre
                style={{
                  marginBottom: 0,
                  maxHeight: 300,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                  fontSize: 12,
                  background: detailModal.statusCode >= 400 ? '#fff2f0' : '#f6f8fa',
                  padding: 12,
                  borderRadius: 6,
                  border: `1px solid ${detailModal.statusCode >= 400 ? '#ffccc7' : '#d0d7de'}`,
                  color: detailModal.statusCode >= 400 ? '#cf1322' : '#24292f',
                  fontFamily: 'Monaco, Menlo, Consolas, monospace',
                }}
              >
                {formatJson(detailModal.response) || '(无响应内容)'}
              </pre>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </PageContainer>
  );
};

export default CommandHistory;
