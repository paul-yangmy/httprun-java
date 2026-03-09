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
      styles={{
        header: {
          borderBottom: '1px solid rgba(128,128,128,0.12)',
          padding: '0 20px',
          minHeight: 56,
          display: 'flex',
          alignItems: 'center',
        },
        body: { padding: '16px 20px' },
      }}
      style={{ borderRadius: 10, boxShadow: '0 1px 8px rgba(0,0,0,0.06)' }}
      title={
        <Space size={12} wrap>
          <span style={{ fontWeight: 600, fontSize: 15 }}>{title}</span>
          {showSearch && (
            <Input
              prefix={<SearchOutlined style={{ color: '#94A3B8' }} />}
              placeholder={searchPlaceholder}
              value={searchValue}
              onChange={(e) => onSearchChange?.(e.target.value)}
              style={{ width: 220, height: 34, borderRadius: 8 }}
              allowClear
              variant="filled"
            />
          )}
        </Space>
      }
      extra={
        <Space size={8}>
          {extra}
          {onRefresh && (
            <Tooltip title="刷新">
              <Button
                type="text"
                icon={<ReloadOutlined spin={loading} style={{ fontSize: 14 }} />}
                onClick={onRefresh}
                disabled={loading}
                style={{ width: 32, height: 32, borderRadius: 6, padding: 0 }}
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
