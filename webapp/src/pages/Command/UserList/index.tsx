import React, { useCallback, useEffect, useMemo, useState } from 'react';
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
  Alert,
  message,
} from 'antd';
import {
  PlayCircleOutlined,
  SearchOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer } from '@ant-design/pro-components';
import { getUserCommandList } from '@/services/httprun';
import CommandExecutor from '@/components/Command/Executor';
import styles from './index.module.less';

const { Text } = Typography;

const UserCommandList: React.FC = () => {
  const [items, setItems] = useState<HTTPRUN.CommandItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [currentCommand, setCurrentCommand] = useState<HTTPRUN.CommandItem | null>(null);
  const [searchText, setSearchText] = useState<string>('');

  const refresh = useCallback(() => {
    setLoading(true);
    getUserCommandList()
      .then((data) => {
        setItems(data || []);
        setLoading(false);
      })
      .catch(() => {
        message.error('获取命令列表失败');
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const filteredItems = useMemo(() => {
    if (!searchText) return items;
    const lowerSearch = searchText.toLowerCase();
    return items.filter(
      (item) =>
        item.name.toLowerCase().includes(lowerSearch) ||
        item.description.toLowerCase().includes(lowerSearch),
    );
  }, [items, searchText]);

  const columns: ColumnsType<HTTPRUN.CommandItem> = [
    {
      title: '命令名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      render: (name: string) => (
        <Space>
          <ThunderboltOutlined style={{ color: '#1677ff' }} />
          <Text strong style={{ color: '#1677ff' }}>
            {name}
          </Text>
        </Space>
      ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (desc: string) => (
        <Tooltip title={desc}>
          <span>{desc || '-'}</span>
        </Tooltip>
      ),
    },
    {
      title: '参数',
      key: 'params',
      width: 200,
      render: (_, record) => {
        const cfg = record.commandConfig || record.command;
        const params = cfg?.params || [];
        if (params.length === 0) {
          return <Text type="secondary">无参数</Text>;
        }
        return (
          <Space wrap size={[4, 4]}>
            {params.slice(0, 3).map((p) => (
              <Tag key={p.name} color={p.required ? 'red' : 'default'}>
                {p.name}
              </Tag>
            ))}
            {params.length > 3 && (
              <Tooltip title={params.slice(3).map((p) => p.name).join(', ')}>
                <Tag>+{params.length - 3}</Tag>
              </Tooltip>
            )}
          </Space>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      align: 'center',
      render: (_, record) => (
        <Button
          type="primary"
          icon={<PlayCircleOutlined />}
          onClick={() => setCurrentCommand(record)}
        >
          运行命令
        </Button>
      ),
    },
  ];

  return (
    <PageContainer>
      {currentCommand && (
        <CommandExecutor
          open
          command={currentCommand}
          onClose={() => setCurrentCommand(null)}
        />
      )}

      <Alert
        message="命令使用说明"
        description="以下是您有权执行的命令列表。点击「运行命令」按钮，填写必要参数后即可执行。"
        type="info"
        showIcon
        icon={<InfoCircleOutlined />}
        closable
        style={{ marginBottom: 16 }}
      />

      <Card
        title={
          <Space>
            <ThunderboltOutlined style={{ color: '#1677ff' }} />
            <span>我的命令</span>
            <Tag color="blue">{filteredItems.length} 条</Tag>
          </Space>
        }
        extra={
          <Space>
            <Input
              placeholder="搜索命令"
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
          dataSource={filteredItems}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
          }}
          locale={{
            emptyText: (
              <Empty description="暂无可用命令" image={Empty.PRESENTED_IMAGE_SIMPLE}>
                <Text type="secondary">请联系管理员为您分配命令权限</Text>
              </Empty>
            ),
          }}
        />
      </Card>
    </PageContainer>
  );
};

export default UserCommandList;
