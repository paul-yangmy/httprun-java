import React, { useCallback, useEffect, useMemo, useState } from 'react';
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
  message,
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer } from '@ant-design/pro-components';
import { getCommandList, deleteCommand } from '@/services/httprun';
import CommandExecutor from '@/components/Command/Executor';
import CommandEditor from '@/components/Command/Editor';
import styles from './index.module.less';

const { Text } = Typography;

const AdminCommand: React.FC = () => {
  const [items, setItems] = useState<HTTPRUN.CommandItem[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [currentCommand, setCurrentCommand] = useState<HTTPRUN.CommandItem | null>(null);
  const [editCommand, setEditCommand] = useState<HTTPRUN.CommandItem | null | Record<string, never>>(null);
  const [searchText, setSearchText] = useState<string>('');

  const refresh = useCallback(() => {
    setLoading(true);
    getCommandList()
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

  const handleDelete = useCallback(
    (name: string) => {
      message.loading({ content: '正在删除...', key: 'delete' });
      deleteCommand([name])
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
        <Text strong style={{ color: '#1677ff' }}>
          {name}
        </Text>
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
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      width: 200,
      render: (path: string) => (
        <Text code copyable={{ text: path }}>
          {path}
        </Text>
      ),
    },
    {
      title: '参数数量',
      key: 'params',
      width: 100,
      align: 'center',
      render: (_, record) => {
        const cfg = record.commandConfig || record.command;
        return <Tag color="blue">{cfg?.params?.length || 0} 个</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      align: 'center',
      render: (status: number) => (
        <Tag color={status === 0 ? 'success' : 'default'}>
          {status === 0 ? '启用' : '停用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="primary"
            size="small"
            icon={<PlayCircleOutlined />}
            onClick={() => setCurrentCommand(record)}
          >
            运行
          </Button>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => setEditCommand(record)}
          >
            编辑
          </Button>
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                {
                  label: '确认删除',
                  key: 'confirm',
                  danger: true,
                  onClick: () => handleDelete(record.name),
                },
              ],
            }}
          >
            <Button size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Dropdown>
        </Space>
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
      {editCommand && (
        <CommandEditor
          open
          command={editCommand as HTTPRUN.CommandItem}
          onClose={() => setEditCommand(null)}
          onChange={refresh}
        />
      )}
      <Card
        title={
          <Space>
            <span>命令管理</span>
            <Tag color="blue">{filteredItems.length} 条</Tag>
          </Space>
        }
        extra={
          <Space>
            <Input
              placeholder="搜索命令名称或描述"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 220 }}
              allowClear
            />
            <Button icon={<ReloadOutlined />} onClick={refresh}>
              刷新
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setEditCommand({})}
            >
              添加命令
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
            emptyText: <Empty description="暂无命令" />,
          }}
        />
      </Card>
    </PageContainer>
  );
};

export default AdminCommand;
