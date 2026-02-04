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
  Modal,
  Alert,
} from 'antd';
import {
  DeleteOutlined,
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  KeyOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer } from '@ant-design/pro-components';
import dayjs from 'dayjs';

const { Text } = Typography;
import { getTokenList, deleteToken } from '@/services/httprun';
import AddTokenModal from '@/components/Token/AddTokenModal';

const { Text, Paragraph } = Typography;

// 星期几名称映射
const WEEKDAY_NAMES: Record<string, string> = {
  '1': '周一',
  '2': '周二',
  '3': '周三',
  '4': '周四',
  '5': '周五',
  '6': '周六',
  '7': '周日',
};

const AdminToken: React.FC = () => {
  const [tokenList, setTokenList] = useState<HTTPRUN.Token[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [page, setPage] = useState({ pageIndex: 1, pageSize: 10 });
  const [showAddTokenModal, setShowAddTokenModal] = useState(false);
  const [searchText, setSearchText] = useState<string>('');
  const [newAdminTokenModal, setNewAdminTokenModal] = useState<{
    visible: boolean;
    token?: HTTPRUN.RevokeTokenResponse;
  }>({ visible: false });

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
    (id: number, isAdmin: boolean) => {
      // 管理员 Token 删除需要二次确认
      if (isAdmin) {
        Modal.confirm({
          title: '确认删除管理员 Token？',
          icon: <ExclamationCircleOutlined />,
          content: (
            <div>
              <p>删除管理员 Token 后，系统将<strong>自动生成新的管理员 Token</strong>。</p>
              <p style={{ color: '#ff4d4f' }}>请确保在关闭弹窗前保存新 Token！</p>
            </div>
          ),
          okText: '确认删除',
          okType: 'danger',
          cancelText: '取消',
          onOk: () => performDelete(id),
        });
      } else {
        performDelete(id);
      }
    },
    [],
  );

  const performDelete = useCallback(
    (id: number) => {
      message.loading({ content: '正在删除...', key: 'delete' });
      deleteToken(id)
        .then((response) => {
          if (response.newAdminTokenGenerated) {
            // 管理员 Token 被删除，显示新 Token 弹窗
            message.success({ content: '管理员 Token 已删除，请保存新 Token！', key: 'delete' });
            setNewAdminTokenModal({ visible: true, token: response });
          } else {
            message.success({ content: '删除成功', key: 'delete' });
          }
          refresh();
        })
        .catch(() => {
          message.error({ content: '删除失败', key: 'delete' });
        });
    },
    [refresh],
  );

  const handleCopyNewToken = useCallback(() => {
    if (newAdminTokenModal.token?.newJwtToken) {
      navigator.clipboard.writeText(newAdminTokenModal.token.newJwtToken);
      message.success('新管理员 Token 已复制到剪贴板');
    }
  }, [newAdminTokenModal.token]);

  const isExpired = (expiresAt: number | null) => {
    if (expiresAt === null || expiresAt === undefined) {
      return false; // 永久 token 不过期
    }
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
      width: 70,
      align: 'center',
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 140,
      ellipsis: true,
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
      width: 180,
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
      width: 180,
      responsive: ['md'] as any,
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
      render: (time: number | null) => {
        if (time === null || time === undefined) {
          return (
            <Space>
              <Badge status="success" />
              <Text strong style={{ color: '#52c41a' }}>永久有效</Text>
            </Space>
          );
        }
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
        if (record.expiresAt === null || record.expiresAt === undefined) {
          return <Tag color="success">永久</Tag>;
        }
        const expired = isExpired(record.expiresAt);
        return (
          <Tag color={expired ? 'error' : 'success'}>
            {expired ? '已过期' : '有效'}
          </Tag>
        );
      },
    },
    {
      title: '时间限制',
      key: 'timeRestriction',
      width: 180,
      render: (_, record) => {
        const hasTimeRange = record.allowedStartTime && record.allowedEndTime;
        const hasWeekdays = record.allowedWeekdays;
        
        if (!hasTimeRange && !hasWeekdays) {
          return <Text type="secondary">无限制</Text>;
        }

        const weekdayNames = hasWeekdays
          ? record.allowedWeekdays!.split(',').map((d) => WEEKDAY_NAMES[d.trim()] || d).join('、')
          : null;

        return (
          <Tooltip
            title={
              <div>
                {hasTimeRange && (
                  <div>时段：{record.allowedStartTime} - {record.allowedEndTime}</div>
                )}
                {weekdayNames && <div>星期：{weekdayNames}</div>}
              </div>
            }
          >
            <Space direction="vertical" size={0}>
              {hasTimeRange && (
                <Tag icon={<ClockCircleOutlined />} color="processing">
                  {record.allowedStartTime} - {record.allowedEndTime}
                </Tag>
              )}
              {weekdayNames && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {weekdayNames.length > 12 ? weekdayNames.substring(0, 12) + '...' : weekdayNames}
                </Text>
              )}
            </Space>
          </Tooltip>
        );
      },
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      width: 140,
      ellipsis: true,
      responsive: ['lg'] as any,
      render: (remark: string) => 
        remark ? (
          <Tooltip title={remark}>
            <Text>{remark}</Text>
          </Tooltip>
        ) : (
          <Text type="secondary">-</Text>
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 90,
      fixed: 'right' as any,
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
                onClick: () => handleDelete(record.id, record.isAdmin),
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
      {/* 新管理员 Token 弹窗 */}
      <Modal
        title={
          <Space>
            <ExclamationCircleOutlined style={{ color: '#faad14' }} />
            <span>新管理员 Token 已生成</span>
          </Space>
        }
        open={newAdminTokenModal.visible}
        onCancel={() => setNewAdminTokenModal({ visible: false })}
        footer={[
          <Button key="copy" type="primary" icon={<CopyOutlined />} onClick={handleCopyNewToken}>
            复制 Token
          </Button>,
          <Button key="close" onClick={() => setNewAdminTokenModal({ visible: false })}>
            我已保存，关闭
          </Button>,
        ]}
        width={700}
        closable={false}
        maskClosable={false}
      >
        <Alert
          message="请立即保存新的管理员 Token！"
          description="关闭此弹窗后将无法再次查看完整 Token 内容。"
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary">Token ID：</Text>
          <Text strong>{newAdminTokenModal.token?.newTokenId}</Text>
        </div>
        <div style={{ marginBottom: 16 }}>
          <Text type="secondary">Token 名称：</Text>
          <Text strong>{newAdminTokenModal.token?.newTokenName}</Text>
        </div>
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">JWT Token：</Text>
        </div>
        <div
          style={{
            background: '#f5f5f5',
            padding: 12,
            borderRadius: 6,
            wordBreak: 'break-all',
            fontFamily: 'monospace',
            fontSize: 12,
            maxHeight: 200,
            overflow: 'auto',
          }}
        >
          {newAdminTokenModal.token?.newJwtToken}
        </div>
      </Modal>

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
          <Space wrap>
            <Input
              placeholder="搜索 Token"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 180, minWidth: 120 }}
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
              添加
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
          scroll={{ x: 1100 }}
          pagination={{
            pageSize: page.pageSize,
            current: page.pageIndex,
            total: filteredTokenList.length,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
            onChange: (pageIndex, pageSize) => setPage({ pageIndex, pageSize }),
            responsive: true,
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
