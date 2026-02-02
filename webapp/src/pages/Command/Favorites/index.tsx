import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Tag,
  message,
  Tooltip,
  Empty,
  Popconfirm,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  StarFilled,
  SearchOutlined,
  PlayCircleOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { PageContainer } from '@ant-design/pro-components';
import { getCommandList } from '@/services/httprun';
import CommandExecutor from '@/components/Command/Executor';

const { Text } = Typography;

interface FavoriteItem {
  commandName: string;
  addedAt: string;
  note?: string;
}

/** 从 localStorage 获取收藏列表 */
const getFavorites = (): FavoriteItem[] => {
  try {
    const favStr = localStorage.getItem('command_favorites') || '[]';
    return JSON.parse(favStr);
  } catch {
    return [];
  }
};

/** 保存收藏列表到 localStorage */
const saveFavorites = (favorites: FavoriteItem[]) => {
  localStorage.setItem('command_favorites', JSON.stringify(favorites));
};

/** 检查命令是否已收藏 */
export const isFavorite = (commandName: string): boolean => {
  const favorites = getFavorites();
  return favorites.some((f) => f.commandName === commandName);
};

/** 添加收藏 */
export const addFavorite = (commandName: string, note?: string) => {
  const favorites = getFavorites();
  if (!favorites.some((f) => f.commandName === commandName)) {
    favorites.push({
      commandName,
      addedAt: new Date().toISOString(),
      note,
    });
    saveFavorites(favorites);
    return true;
  }
  return false;
};

/** 移除收藏 */
export const removeFavorite = (commandName: string) => {
  const favorites = getFavorites();
  const newFavorites = favorites.filter((f) => f.commandName !== commandName);
  saveFavorites(newFavorites);
};

/** 切换收藏状态 */
export const toggleFavorite = (commandName: string): boolean => {
  if (isFavorite(commandName)) {
    removeFavorite(commandName);
    return false;
  } else {
    addFavorite(commandName);
    return true;
  }
};

interface FavoriteCommandItem extends HTTPRUN.CommandItem {
  addedAt: string;
  note?: string;
}

const CommandFavorites: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [commands, setCommands] = useState<FavoriteCommandItem[]>([]);
  const [searchText, setSearchText] = useState('');
  const [executorOpen, setExecutorOpen] = useState(false);
  const [selectedCommand, setSelectedCommand] = useState<HTTPRUN.CommandItem | null>(null);

  const loadFavorites = useCallback(async () => {
    setLoading(true);
    try {
      const favorites = getFavorites();
      if (favorites.length === 0) {
        setCommands([]);
        setLoading(false);
        return;
      }

      // 获取所有命令列表
      const allCommands = await getCommandList();

      // 匹配收藏的命令
      const favoriteCommands: FavoriteCommandItem[] = [];
      favorites.forEach((fav) => {
        const cmd = allCommands.find((c) => c.name === fav.commandName);
        if (cmd) {
          favoriteCommands.push({
            ...cmd,
            addedAt: fav.addedAt,
            note: fav.note,
          });
        }
      });

      setCommands(favoriteCommands);
    } catch (error) {
      message.error('加载收藏失败');
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    loadFavorites();
  }, [loadFavorites]);

  const handleRemoveFavorite = (commandName: string) => {
    removeFavorite(commandName);
    setCommands(commands.filter((c) => c.name !== commandName));
    message.success('已取消收藏');
  };

  const handleExecute = (command: HTTPRUN.CommandItem) => {
    setSelectedCommand(command);
    setExecutorOpen(true);
  };

  const filteredCommands = commands.filter(
    (cmd) =>
      cmd.name.toLowerCase().includes(searchText.toLowerCase()) ||
      cmd.description?.toLowerCase().includes(searchText.toLowerCase()),
  );

  const columns: ColumnsType<FavoriteCommandItem> = [
    {
      title: '命令名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string) => (
        <Space>
          <StarFilled style={{ color: '#faad14' }} />
          <Text strong copyable>
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
      render: (text: string) => text || <Text type="secondary">-</Text>,
    },
    {
      title: '标签',
      dataIndex: 'tags',
      key: 'tags',
      width: 200,
      render: (tags: string[]) =>
        tags && tags.length > 0 ? (
          <Space size={4} wrap>
            {tags.map((tag) => (
              <Tag key={tag} color="blue">
                {tag}
              </Tag>
            ))}
          </Space>
        ) : (
          <Text type="secondary">-</Text>
        ),
    },
    {
      title: '收藏时间',
      dataIndex: 'addedAt',
      key: 'addedAt',
      width: 180,
      render: (time: string) => {
        const date = new Date(time);
        return date.toLocaleString('zh-CN');
      },
      sorter: (a, b) => new Date(a.addedAt).getTime() - new Date(b.addedAt).getTime(),
      defaultSortOrder: 'descend',
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <Space>
          <Tooltip title="执行命令">
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              size="small"
              onClick={() => handleExecute(record)}
            >
              执行
            </Button>
          </Tooltip>
          <Popconfirm
            title="确定取消收藏此命令吗？"
            onConfirm={() => handleRemoveFavorite(record.name)}
            okText="确定"
            cancelText="取消"
          >
            <Tooltip title="取消收藏">
              <Button danger icon={<DeleteOutlined />} size="small">
                取消
              </Button>
            </Tooltip>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <PageContainer
      header={{
        title: '我的收藏',
        subTitle: '快速访问常用命令',
      }}
    >
      <Card>
        <Space style={{ marginBottom: 16 }} size="middle">
          <Input
            placeholder="搜索命令名称或描述"
            prefix={<SearchOutlined />}
            allowClear
            style={{ width: 300 }}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
          <Button icon={<ReloadOutlined />} onClick={loadFavorites}>
            刷新
          </Button>
        </Space>

        <Table
          columns={columns}
          dataSource={filteredCommands}
          rowKey="name"
          loading={loading}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            defaultPageSize: 10,
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="暂无收藏的命令"
              >
                <Button type="primary" href="/">
                  去添加收藏
                </Button>
              </Empty>
            ),
          }}
        />
      </Card>

      {selectedCommand && (
        <CommandExecutor
          open={executorOpen}
          command={selectedCommand}
          onClose={() => {
            setExecutorOpen(false);
            setSelectedCommand(null);
          }}
        />
      )}
    </PageContainer>
  );
};

export default CommandFavorites;
