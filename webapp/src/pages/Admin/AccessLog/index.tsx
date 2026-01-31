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
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer } from '@ant-design/pro-components';
import dayjs from 'dayjs';
import { getAccessLogList } from '@/services/httprun';

const { Text } = Typography;

const AdminAccessLog: React.FC = () => {
  const [data, setData] = useState<HTTPRUN.AccessLogListResponse>();
  const [loading, setLoading] = useState<boolean>(false);
  const [page, setPage] = useState({ pageIndex: 1, pageSize: 10 });
  const [searchText, setSearchText] = useState<string>('');
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

  const columns: ColumnsType<HTTPRUN.AccessLogItem> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      align: 'center',
    },
    {
      title: '请求路径',
      dataIndex: 'path',
      key: 'path',
      width: 200,
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
      width: 140,
      render: (ip: string) => <Tag color="blue">{ip}</Tag>,
    },
    {
      title: 'Token ID',
      dataIndex: 'token_id',
      key: 'token_id',
      width: 120,
      ellipsis: true,
      render: (tokenId: string) => (
        <Tooltip title={tokenId}>
          <Text type="secondary">{tokenId?.substring(0, 12)}...</Text>
        </Tooltip>
      ),
    },
    {
      title: '请求内容',
      dataIndex: 'request',
      key: 'request',
      width: 200,
      ellipsis: true,
      render: (request: string) => (
        <Tooltip title={request}>
          <Text ellipsis style={{ maxWidth: 180 }}>
            {request || '-'}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: '访问时间',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (time: string) => (
        <Text type="secondary">
          {dayjs(time).format('YYYY-MM-DD HH:mm:ss')}
        </Text>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
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
        </Space>
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
            <Input
              placeholder="搜索路径或 IP"
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
          dataSource={data?.items}
          rowKey="id"
          loading={loading}
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
        width={700}
      >
        {detailModal && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="ID">{detailModal.id}</Descriptions.Item>
            <Descriptions.Item label="请求路径">
              <Text code copyable>
                {detailModal.path}
              </Text>
            </Descriptions.Item>
            <Descriptions.Item label="客户端 IP">
              <Tag color="blue">{detailModal.ip}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Token ID">
              <Text copyable>{detailModal.token_id}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="请求内容">
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
            <Descriptions.Item label="响应内容">
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
            <Descriptions.Item label="访问时间">
              {dayjs(detailModal.created_at).format('YYYY-MM-DD HH:mm:ss')}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </PageContainer>
  );
};

export default AdminAccessLog;
