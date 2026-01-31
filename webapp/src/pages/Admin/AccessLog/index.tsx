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
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  GlobalOutlined,
  ApiOutlined,
  CodeOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer } from '@ant-design/pro-components';
import dayjs from 'dayjs';
import { getAccessLogList } from '@/services/httprun';

const { Text } = Typography;

// 来源类型对应的图标和颜色
const sourceConfig: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  WEB: { icon: <GlobalOutlined />, color: 'blue', label: 'Web 浏览器' },
  API: { icon: <ApiOutlined />, color: 'green', label: 'API 调用' },
  CLI: { icon: <CodeOutlined />, color: 'orange', label: '命令行工具' },
};

const AdminAccessLog: React.FC = () => {
  const [data, setData] = useState<HTTPRUN.AccessLogListResponse>();
  const [loading, setLoading] = useState<boolean>(false);
  const [page, setPage] = useState({ pageIndex: 1, pageSize: 10 });
  const [searchText, setSearchText] = useState<string>('');
  const [sourceFilter, setSourceFilter] = useState<string>('');
  const [detailModal, setDetailModal] = useState<HTTPRUN.AccessLogItem | null>(null);

  const refresh = useCallback(() => {
    setLoading(true);
    getAccessLogList(page)
      .then((response) => {
        setLoading(false);
        setData(response);
      })
      .catch(() => {
        message.error('获取访问日志失败');
        setLoading(false);
      });
  }, [page]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  // 过滤数据
  const filteredData = data?.items?.filter((item) => {
    const matchSearch = !searchText || 
      item.path?.toLowerCase().includes(searchText.toLowerCase()) ||
      item.ip?.toLowerCase().includes(searchText.toLowerCase()) ||
      item.request_id?.toLowerCase().includes(searchText.toLowerCase());
    const matchSource = !sourceFilter || item.source === sourceFilter;
    return matchSearch && matchSource;
  });

  const columns: ColumnsType<HTTPRUN.AccessLogItem> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 70,
      align: 'center',
    },
    {
      title: '请求 ID',
      dataIndex: 'request_id',
      key: 'request_id',
      width: 140,
      render: (requestId: string) => (
        <Tooltip title={requestId}>
          <Text code style={{ fontSize: 11 }}>{requestId || '-'}</Text>
        </Tooltip>
      ),
    },
    {
      title: '来源',
      dataIndex: 'source',
      key: 'source',
      width: 100,
      align: 'center',
      render: (source: string) => {
        const config = sourceConfig[source] || { color: 'default', label: source || '未知' };
        return (
          <Tag color={config.color} icon={config.icon}>
            {config.label}
          </Tag>
        );
      },
    },
    {
      title: '命令/接口',
      dataIndex: 'command_name',
      key: 'command_name',
      width: 150,
      ellipsis: true,
      render: (name: string) => (
        <Tooltip title={name}>
          <Text type="secondary">{name || '-'}</Text>
        </Tooltip>
      ),
    },
    {
      title: '请求路径',
      dataIndex: 'path',
      key: 'path',
      width: 180,
      ellipsis: true,
      render: (path: string) => (
        <Tooltip title={path}>
          <Text code>{path}</Text>
        </Tooltip>
      ),
    },
    {
      title: '客户端 IP',
      dataIndex: 'ip',
      key: 'ip',
      width: 130,
      render: (ip: string) => <Tag color="blue">{ip}</Tag>,
    },
    {
      title: 'Token ID',
      dataIndex: 'token_id',
      key: 'token_id',
      width: 100,
      ellipsis: true,
      render: (tokenId: string) => (
        <Tooltip title={tokenId}>
          <Text type="secondary">{tokenId?.substring(0, 10)}...</Text>
        </Tooltip>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status_code',
      key: 'status_code',
      width: 80,
      align: 'center',
      render: (code: number) => (
        <Tag color={code >= 200 && code < 300 ? 'success' : code >= 400 ? 'error' : 'warning'}>
          {code}
        </Tag>
      ),
    },
    {
      title: '耗时',
      dataIndex: 'duration',
      key: 'duration',
      width: 80,
      align: 'right',
      render: (duration: number) => (
        <Text type={duration > 1000 ? 'danger' : 'secondary'}>
          {duration}ms
        </Text>
      ),
    },
    {
      title: '访问时间',
      dataIndex: 'created_at',
      key: 'created_at',
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
      width: 70,
      align: 'center',
      fixed: 'right',
      render: (_, record) => (
        <Tooltip title="查看详情">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => setDetailModal(record)}
          />
        </Tooltip>
      ),
    },
  ];

  return (
    <PageContainer>
      <Card
        title={
          <Space>
            <span>访问日志</span>
            <Tag color="blue">{data?.total || 0} 条</Tag>
          </Space>
        }
        extra={
          <Space>
            <Select
              placeholder="来源筛选"
              allowClear
              style={{ width: 140 }}
              value={sourceFilter || undefined}
              onChange={(value) => setSourceFilter(value || '')}
              options={[
                { value: 'WEB', label: 'Web 浏览器' },
                { value: 'API', label: 'API 调用' },
                { value: 'CLI', label: '命令行工具' },
              ]}
            />
            <Input
              placeholder="搜索路径/IP/请求ID"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 200 }}
              allowClear
            />
            <Button icon={<ReloadOutlined />} onClick={refresh}>
              刷新
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
          scroll={{ x: 1400 }}
          pagination={{
            pageSize: page.pageSize,
            current: page.pageIndex,
            total: data?.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
            onChange: (pageIndex, pageSize) => setPage({ pageIndex, pageSize }),
          }}
          locale={{
            emptyText: <Empty description="暂无访问日志" />,
          }}
        />
      </Card>

      <Modal
        title="访问日志详情"
        open={!!detailModal}
        onCancel={() => setDetailModal(null)}
        footer={[
          <Button key="close" onClick={() => setDetailModal(null)}>
            关闭
          </Button>,
        ]}
        width={800}
      >
        {detailModal && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="ID">{detailModal.id}</Descriptions.Item>
            <Descriptions.Item label="请求 ID">
              <Text code copyable>{detailModal.request_id || '-'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="来源">
              {(() => {
                const config = sourceConfig[detailModal.source || ''] || { color: 'default', label: detailModal.source || '未知' };
                return <Tag color={config.color} icon={config.icon}>{config.label}</Tag>;
              })()}
            </Descriptions.Item>
            <Descriptions.Item label="命令/接口">
              <Text code>{detailModal.command_name || '-'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="请求路径" span={2}>
              <Text code copyable>
                {detailModal.path}
              </Text>
            </Descriptions.Item>
            <Descriptions.Item label="客户端 IP">
              <Tag color="blue">{detailModal.ip}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="转发 IP">
              <Text type="secondary">{detailModal.forwarded_for || '-'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Token ID" span={2}>
              <Text copyable>{detailModal.token_id || '-'}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="User-Agent" span={2}>
              <Text type="secondary" style={{ fontSize: 12, wordBreak: 'break-all' }}>
                {detailModal.user_agent || '-'}
              </Text>
            </Descriptions.Item>
            <Descriptions.Item label="来源页面" span={2}>
              <Text type="secondary" style={{ fontSize: 12, wordBreak: 'break-all' }}>
                {detailModal.referer || '-'}
              </Text>
            </Descriptions.Item>
            <Descriptions.Item label="状态码">
              <Tag color={detailModal.status_code >= 200 && detailModal.status_code < 300 ? 'success' : 'error'}>
                {detailModal.status_code}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="耗时">
              <Text type={detailModal.duration > 1000 ? 'danger' : 'success'}>
                {detailModal.duration}ms
              </Text>
            </Descriptions.Item>
            <Descriptions.Item label="请求内容" span={2}>
              <pre
                style={{
                  maxHeight: 200,
                  overflow: 'auto',
                  backgroundColor: '#f5f5f5',
                  padding: 12,
                  borderRadius: 6,
                  margin: 0,
                  fontSize: 12,
                }}
              >
                {detailModal.request || '-'}
              </pre>
            </Descriptions.Item>
            <Descriptions.Item label="响应内容" span={2}>
              <pre
                style={{
                  maxHeight: 200,
                  overflow: 'auto',
                  backgroundColor: '#f5f5f5',
                  padding: 12,
                  borderRadius: 6,
                  margin: 0,
                  fontSize: 12,
                }}
              >
                {detailModal.response || '-'}
              </pre>
            </Descriptions.Item>
            <Descriptions.Item label="访问时间" span={2}>
              {dayjs(detailModal.created_at).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </PageContainer>
  );
};

export default AdminAccessLog;
