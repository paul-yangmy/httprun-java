import React from 'react';
import { Space, Button, Dropdown } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';

interface ActionButtonsProps {
  onEdit?: () => void;
  onDelete?: () => void;
  onView?: () => void;
  onExecute?: () => void;
  editText?: string;
  deleteText?: string;
  viewText?: string;
  executeText?: string;
  extraActions?: React.ReactNode;
  showEdit?: boolean;
  showDelete?: boolean;
  showView?: boolean;
  showExecute?: boolean;
}

/**
 * 操作按钮组件
 * 提供统一的表格操作按钮样式
 */
const ActionButtons: React.FC<ActionButtonsProps> = ({
  onEdit,
  onDelete,
  onView,
  onExecute,
  editText = '编辑',
  deleteText = '删除',
  viewText = '查看',
  executeText = '执行',
  extraActions,
  showEdit = true,
  showDelete = true,
  showView = false,
  showExecute = false,
}) => {
  const deleteMenuItems: MenuProps['items'] = [
    {
      label: `确认${deleteText}`,
      key: 'confirm',
      danger: true,
      onClick: onDelete,
    },
  ];

  return (
    <Space size="small">
      {showView && onView && (
        <Button size="small" onClick={onView}>
          {viewText}
        </Button>
      )}
      {showExecute && onExecute && (
        <Button type="primary" size="small" onClick={onExecute}>
          {executeText}
        </Button>
      )}
      {showEdit && onEdit && (
        <Button size="small" onClick={onEdit}>
          {editText}
        </Button>
      )}
      {extraActions}
      {showDelete && onDelete && (
        <Dropdown trigger={['click']} menu={{ items: deleteMenuItems }}>
          <Button size="small" danger icon={<DeleteOutlined />}>
            {deleteText}
          </Button>
        </Dropdown>
      )}
    </Space>
  );
};

export default ActionButtons;
