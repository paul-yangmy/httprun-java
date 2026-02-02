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
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer } from '@ant-design/pro-components';
import dayjs from 'dayjs';
import { getUserCommandList, runCommand } from '@/services/httprun';

const { Text, Paragraph } = Typography;
const { RangePicker } = DatePicker;

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

const CommandHistory: React.FC = () => {
  const [data, setData] = useState<ExecutionHistoryItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [searchText, setSearchText] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs | null, dayjs.Dayjs | null]>([null, null]);
  const [detailModal, setDetailModal] = useState<ExecutionHistoryItem | null>(null);
  const [commandList, setCommandList] = useState<HTTPRUN.CommandItem[]>([]);
  const [commandFilter, setCommandFilter] = useState<string>('');

  // 从 localStorage 获取执行历史
  const loadHistory = useCallback(() => {
    setLoading(true);
    try {
      const historyStr = localStorage.getItem('command_execution_history') || '[]';
      const history: ExecutionHistoryItem[] = JSON.parse(historyStr);
      // 按时间倒序排列
      history.sort((a, b) => new Date(b.executedAt).getTime() - new Date(a.executedAt).getTime());
      setData(history);
    } catch (e) {
      setData([]);
    }
    setLoading(false);
  }, []);

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

  // 过滤数据
  const filteredData = data.filter((item) => {
    const matchSearch =
      !searchText ||
      item.commandName?.toLowerCase().includes(searchText.toLowerCase()) ||
      item.params?.toLowerCase().includes(searchText.toLowerCase());
    const matchStatus = !statusFilter || item.status === statusFilter;
    const matchCommand = !commandFilter || item.commandName === commandFilter;
    
    let matchDate = true;
    if (dateRange[0] && dateRange[1]) {
      const itemDate = dayjs(item.executedAt);
      matchDate = itemDate.isAfter(dateRange[0].startOf('day')) && 
                  itemDate.isBefore(dateRange[1].endOf('day'));
    }
    
    return matchSearch && matchStatus && matchCommand && matchDate;
  });

  // 清空历史记录
  const handleClearHistory = () => {
    Modal.confirm({
      title: '确认清空',
      content: '确定要清空所有执行历史记录吗？此操作不可恢复。',
      okText: '确认清空',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => {
        localStorage.removeItem('command_execution_history');
        setData([]);
        message.success('历史记录已清空');
      },
    });
  };

  // 重新执行命令
  const handleRerun = (item: ExecutionHistoryItem) => {
    const command = commandList.find((c) => c.name === item.commandName);
    if (!command) {
      message.warning('命令不存在或您没有执行权限');
      return;
    }
    
    // 解析参数
    let params: HTTPRUN.Param[] = [];
    try {
      params = JSON.parse(item.params || '[]');
    } catch {
      params = [];
    }
    
    message.loading({ content: '正在执行...', key: 'rerun' });
    
    runCommand(item.commandName, params, [])
      .then((res) => {
        const newItem: ExecutionHistoryItem = {
          id: Date.now(),
          commandName: item.commandName,
          params: item.params,
          status: res.error ? 'error' : 'success',
          stdout: res.stdout || '',
          stderr: res.stderr || '',
          duration: 0,
          executedAt: new Date().toISOString(),
          ip: '-',
        };
        
        // 保存到历史
        const history = [...data, newItem];
        localStorage.setItem('command_execution_history', JSON.stringify(history));
        setData([newItem, ...data]);
        
        message.success({ content: '执行完成', key: 'rerun' });
      })
      .catch(() => {
        message.error({ content: '执行失败', key: 'rerun' });
      });
  };

  const getStatusTag = (status: string) => {
    switch (status) {
      case 'success':
        return <Tag icon={<CheckCircleOutlined />} color="success">成功</Tag>;
      case 'error':
        return <Tag icon={<CloseCircleOutlined />} color="error">失败</Tag>;
      case 'running':
        return <Tag icon={<ClockCircleOutlined />} color="processing">执行中</Tag>;
      default:
        return <Tag>{status}</Tag>;
    }
  };

  const columns: ColumnsType<ExecutionHistoryItem> = [
    {
      title: '命令名称',
      dataIndex: 'commandName',
      key: 'commandName',
      width: 180,
      render: (name: string) => (
        <Text strong style={{ color: '#1677ff' }}>
          {name}
        </Text>
      ),
    },
    {
      title: '参数',
      dataIndex: 'params',
      key: 'params',
      width: 200,
      ellipsis: true,
      render: (params: string) => {
        if (!params || params === '[]') {
          return <Text type="secondary">无参数</Text>;
        }
        try {
          const parsed = JSON.parse(params);
          const display = parsed.map((p: any) => `${p.name}=${p.value}`).join(', ');
          return (
            <Tooltip title={display}>
              <Text code style={{ fontSize: 12 }}>{display}</Text>
            </Tooltip>
          );
        } catch {
          return <Text type="secondary">-</Text>;
        }
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      align: 'center',
      render: (status: string) => getStatusTag(status),
    },
    {
      title: '耗时',
      dataIndex: 'duration',
      key: 'duration',
      width: 100,
      align: 'right',
      render: (duration: number) => (
        <Text type={duration > 5000 ? 'danger' : 'secondary'}>
          {duration > 0 ? `${duration}ms` : '-'}
        </Text>
      ),
    },
    {
      title: '执行时间',
      dataIndex: 'executedAt',
      key: 'executedAt',
      width: 180,
      render: (time: string) => (
        <Text type="secondary" style={{ fontSize: 12 }}>
          {dayjs(time).format('YYYY-MM-DD HH:mm:ss')}
        </Text>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      align: 'center',
      render: (_, record) => (
        <Space>
          <Tooltip title="查看详情">
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => setDetailModal(record)}
            />
          </Tooltip>
          <Tooltip title="重新执行">
            <Button
              type="link"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={() => handleRerun(record)}
            />
          </Tooltip>
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
            <Badge count={filteredData.length} showZero color="#1677ff" />
          </Space>
        }
        extra={
          <Space wrap>
            <Input
              placeholder="搜索命令或参数"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 160 }}
              allowClear
            />
            <Select
              placeholder="命令筛选"
              value={commandFilter || undefined}
              onChange={setCommandFilter}
              style={{ width: 140 }}
              allowClear
            >
              {commandList.map((cmd) => (
                <Select.Option key={cmd.name} value={cmd.name}>
                  {cmd.name}
                </Select.Option>
              ))}
            </Select>
            <Select
              placeholder="状态筛选"
              value={statusFilter || undefined}
              onChange={setStatusFilter}
              style={{ width: 100 }}
              allowClear
            >
              <Select.Option value="success">成功</Select.Option>
              <Select.Option value="error">失败</Select.Option>
            </Select>
            <RangePicker
              value={dateRange}
              onChange={(dates) => setDateRange(dates as [dayjs.Dayjs | null, dayjs.Dayjs | null])}
              style={{ width: 240 }}
            />
            <Button icon={<ReloadOutlined />} onClick={loadHistory}>
              刷新
            </Button>
            <Button danger onClick={handleClearHistory} disabled={data.length === 0}>
              清空历史
            </Button>
          </Space>
        }
        bordered={false}
      >
        <Table
          columns={columns}
          dataSource={filteredData}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 15,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
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
          <Button key="rerun" type="primary" icon={<PlayCircleOutlined />} onClick={() => {
            if (detailModal) handleRerun(detailModal);
            setDetailModal(null);
          }}>
            重新执行
          </Button>,
          <Button key="close" onClick={() => setDetailModal(null)}>
            关闭
          </Button>,
        ]}
        width={700}
      >
        {detailModal && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="命令名称" span={1}>
              <Text strong>{detailModal.commandName}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="执行状态" span={1}>
              {getStatusTag(detailModal.status)}
            </Descriptions.Item>
            <Descriptions.Item label="执行时间" span={1}>
              {dayjs(detailModal.executedAt).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
            <Descriptions.Item label="耗时" span={1}>
              {detailModal.duration > 0 ? `${detailModal.duration}ms` : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="执行参数" span={2}>
              <Paragraph
                code
                copyable
                style={{ 
                  marginBottom: 0, 
                  maxHeight: 80, 
                  overflow: 'auto',
                  fontSize: 12,
                }}
              >
                {detailModal.params || '无参数'}
              </Paragraph>
            </Descriptions.Item>
            <Descriptions.Item label="标准输出 (stdout)" span={2}>
              <Paragraph
                code
                copyable={!!detailModal.stdout}
                style={{
                  marginBottom: 0,
                  maxHeight: 200,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                  fontSize: 12,
                  background: '#f5f5f5',
                  padding: 8,
                  borderRadius: 4,
                }}
              >
                {detailModal.stdout || '(无输出)'}
              </Paragraph>
            </Descriptions.Item>
            <Descriptions.Item label="标准错误 (stderr)" span={2}>
              <Paragraph
                code
                copyable={!!detailModal.stderr}
                style={{
                  marginBottom: 0,
                  maxHeight: 200,
                  overflow: 'auto',
                  whiteSpace: 'pre-wrap',
                  fontSize: 12,
                  background: detailModal.stderr ? '#fff2f0' : '#f5f5f5',
                  padding: 8,
                  borderRadius: 4,
                  color: detailModal.stderr ? '#ff4d4f' : undefined,
                }}
              >
                {detailModal.stderr || '(无错误)'}
              </Paragraph>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </PageContainer>
  );
};

export default CommandHistory;
