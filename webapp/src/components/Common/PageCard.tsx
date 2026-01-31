import React from 'react';
import { Card, Space, Input, Button, Tooltip } from 'antd';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';

interface PageCardProps {
  title: string;
  extra?: React.ReactNode;
  loading?: boolean;
  showSearch?: boolean;
  searchPlaceholder?: string;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
  onRefresh?: () => void;
  children: React.ReactNode;
}

/**
 * 页面卡片组件
 * 提供统一的页面容器，包含标题、搜索、刷新等通用功能
 */
const PageCard: React.FC<PageCardProps> = ({
  title,
  extra,
  loading = false,
  showSearch = true,
  searchPlaceholder = '搜索...',
  searchValue = '',
  onSearchChange,
  onRefresh,
  children,
}) => {
  return (
    <Card
      title={
        <Space>
          <span>{title}</span>
          {showSearch && (
            <Input
              prefix={<SearchOutlined />}
              placeholder={searchPlaceholder}
              value={searchValue}
              onChange={(e) => onSearchChange?.(e.target.value)}
              style={{ width: 240 }}
              allowClear
            />
          )}
        </Space>
      }
      extra={
        <Space>
          {extra}
          {onRefresh && (
            <Tooltip title="刷新">
              <Button
                type="text"
                icon={<ReloadOutlined spin={loading} />}
                onClick={onRefresh}
                disabled={loading}
              />
            </Tooltip>
          )}
        </Space>
      }
      loading={loading}
    >
      {children}
    </Card>
  );
};

export default PageCard;
