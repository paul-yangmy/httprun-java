import React, { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Dropdown,
  Table,
  Tag,
  Space,
  Input,
  Typography,
  Tooltip,
  Empty,
  Badge,
  message,
} from 'antd';
import {
  DeleteOutlined,
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer } from '@ant-design/pro-components';
import dayjs from 'dayjs';
import { getTokenList, deleteToken } from '@/services/httprun';
import AddTokenModal from '@/components/Token/AddTokenModal';

const { Text, Paragraph } = Typography;

const AdminToken: React.FC = () => {
  const [tokenList, setTokenList] = useState<HTTPRUN.Token[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [page, setPage] = useState({ pageIndex: 1, pageSize: 10 });
  const [showAddTokenModal, setShowAddTokenModal] = useState(false);
  const [searchText, setSearchText] = useState<string>('');

  const refresh = useCallback(() => {
    setLoading(true);
    getTokenList(page)
      .then((data) => {
        // 后端直接返回数组
        setTokenList(data || []);
        setLoading(false);
      })
      .catch(() => {
        message.error('获取 Token 列表失败');
        setLoading(false);
      });
  }, [page]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const handleDelete = useCallback(
    (id: number) => {
      message.loading({ content: '正在删除...', key: 'delete' });
      deleteToken(id)
        .then(() => {
          message.success({ content: '删除成功', key: 'delete' });
          refresh();
        })
        .catch(() => {
          message.error({ content: '删除失败', key: 'delete' });
        });
    },
    [refresh],
  );

  const isExpired = (expiresAt: number) => {
    return expiresAt * 1000 < Date.now();
  };

  // 根据搜索文本过滤 Token 列表
  const filteredTokenList = tokenList.filter((token) => {
    if (!searchText) return true;
    const search = searchText.toLowerCase();
    return (
      token.name?.toLowerCase().includes(search) ||
      token.subject?.toLowerCase().includes(search)
    );
  });

  const columns: ColumnsType<HTTPRUN.Token> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      align: 'center',
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
      render: (name: string) => (
        <Space>
          <KeyOutlined style={{ color: '#1677ff' }} />
          <Text strong>{name}</Text>
        </Space>
      ),
    },
    {
      title: 'Token',
      dataIndex: 'jwtToken',
      key: 'jwtToken',
      width: 200,
      ellipsis: true,
      render: (token: string) => (
        <Tooltip title="点击复制完整 Token">
          <Paragraph
            copyable={{ text: token, tooltips: ['复制', '已复制'] }}
            style={{ marginBottom: 0, maxWidth: 180 }}
            ellipsis
          >
            <Text code>{token?.substring(0, 20)}...</Text>
          </Paragraph>
        </Tooltip>
      ),
    },
    {
      title: '授权命令',
      dataIndex: 'subject',
      key: 'subject',
      width: 200,
      render: (subject: string) => {
        const commands = subject?.split(',').filter(Boolean) || [];
        if (commands.length === 0) return <Text type="secondary">-</Text>;
        return (
          <Space wrap size={[4, 4]}>
            {commands.slice(0, 3).map((cmd) => (
              <Tag key={cmd} color="blue">
                {cmd}
              </Tag>
            ))}
            {commands.length > 3 && (
              <Tooltip title={commands.slice(3).join(', ')}>
                <Tag>+{commands.length - 3}</Tag>
              </Tooltip>
            )}
          </Space>
        );
      },
    },
    {
      title: '签发时间',
      dataIndex: 'issuedAt',
      key: 'issuedAt',
      width: 170,
      render: (time: number) => (
        <Text type="secondary">
          {dayjs(time * 1000).format('YYYY-MM-DD HH:mm:ss')}
        </Text>
      ),
    },
    {
      title: '过期时间',
      dataIndex: 'expiresAt',
      key: 'expiresAt',
      width: 170,
      render: (time: number) => {
        const expired = isExpired(time);
        return (
          <Space>
            <Badge status={expired ? 'error' : 'success'} />
            <Text type={expired ? 'danger' : 'secondary'}>
              {dayjs(time * 1000).format('YYYY-MM-DD HH:mm:ss')}
            </Text>
          </Space>
        );
      },
    },
    {
      title: '状态',
      key: 'status',
      width: 80,
      align: 'center',
      render: (_, record) => {
        const expired = isExpired(record.expiresAt);
        return (
          <Tag color={expired ? 'error' : 'success'}>
            {expired ? '已过期' : '有效'}
          </Tag>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      align: 'center',
      render: (_, record) => (
        <Dropdown
          trigger={['click']}
          menu={{
            items: [
              {
                label: '确认删除',
                key: 'delete',
                danger: true,
                icon: <DeleteOutlined />,
                onClick: () => handleDelete(record.id),
              },
            ],
          }}
        >
          <Button type="link" danger icon={<DeleteOutlined />}>
            删除
          </Button>
        </Dropdown>
      ),
    },
  ];

  return (
    <PageContainer>
      <AddTokenModal
        open={showAddTokenModal}
        onOpenChange={setShowAddTokenModal}
        onChange={refresh}
      />
      <Card
        title={
          <Space>
            <span>Token 管理</span>
            <Tag color="blue">{tokenList.length || 0} 条</Tag>
          </Space>
        }
        extra={
          <Space>
            <Input
              placeholder="搜索名称或授权命令"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 200 }}
              allowClear
            />
            <Button icon={<ReloadOutlined />} onClick={refresh}>
              刷新
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setShowAddTokenModal(true)}
            >
              添加 Token
            </Button>
          </Space>
        }
        bordered={false}
      >
        <Table
          columns={columns}
          dataSource={filteredTokenList}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: page.pageSize,
            current: page.pageIndex,
            total: filteredTokenList.length,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
            onChange: (pageIndex, pageSize) => setPage({ pageIndex, pageSize }),
          }}
          locale={{
            emptyText: <Empty description="暂无 Token" />,
          }}
        />
      </Card>
    </PageContainer>
  );
};

export default AdminToken;
