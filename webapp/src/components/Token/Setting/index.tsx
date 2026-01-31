import React, { useCallback } from 'react';
import { Form, Input, Modal } from 'antd';

export interface TokenSettingProps {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  onOk?: (token: string) => void;
}

const TokenSetting: React.FC<TokenSettingProps> = ({ open, onOk, onOpenChange }) => {
  const [form] = Form.useForm();

  const handleTokenSave = useCallback(() => {
    form
      .validateFields()
      .then(() => {
        const token = form.getFieldValue('token');
        localStorage.setItem('token', token);
        if (onOk) {
          onOk(token);
        }
      })
      .catch(() => {
        // validation failed
      });
  }, [form, onOk]);

  return (
    <Modal
      open={open}
      title="设置Token"
      okText="保存"
      cancelText="取消"
      onOk={handleTokenSave}
      onCancel={() => onOpenChange && onOpenChange(false)}
      closable={false}
      maskClosable={false}
    >
      <Form form={form} onSubmitCapture={handleTokenSave}>
        <Form.Item name="token" label="Token" rules={[{ required: true, message: '请输入Token' }]}>
          <Input />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default TokenSetting;
