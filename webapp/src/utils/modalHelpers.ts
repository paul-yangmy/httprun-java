import React from 'react';
import { Modal, message } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';

/**
 * 确认删除对话框
 */
export const showDeleteConfirm = (
  title: string,
  content: string,
  onConfirm: () => void | Promise<void>,
  onCancel?: () => void,
) => {
  Modal.confirm({
    title,
    icon: <ExclamationCircleOutlined />,
    content,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await onConfirm();
      } catch (error) {
        message.error('操作失败');
        throw error;
      }
    },
    onCancel,
  });
};

/**
 * 通用删除处理函数
 */
export const handleDelete = async (
  deleteFn: () => Promise<any>,
  onSuccess: () => void,
  successMessage: string = '删除成功',
  errorMessage: string = '删除失败',
) => {
  message.loading({ content: '正在删除...', key: 'delete' });
  try {
    await deleteFn();
    message.success({ content: successMessage, key: 'delete' });
    onSuccess();
  } catch (error) {
    message.error({ content: errorMessage, key: 'delete' });
    throw error;
  }
};

/**
 * 通用表单提交处理
 */
export const handleSubmit = async (
  submitFn: () => Promise<any>,
  onSuccess: () => void,
  successMessage: string = '操作成功',
  errorMessage: string = '操作失败',
) => {
  message.loading({ content: '正在处理...', key: 'submit' });
  try {
    await submitFn();
    message.success({ content: successMessage, key: 'submit' });
    onSuccess();
  } catch (error) {
    message.error({ content: errorMessage, key: 'submit' });
    throw error;
  }
};
