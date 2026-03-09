import React, { useCallback } from 'react';
import { Form, Input, Modal, Typography } from 'antd';
import { KeyOutlined, LockOutlined } from '@ant-design/icons';

const { Text } = Typography;

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
      title={
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{
            width: 32, height: 32, borderRadius: 8,
            background: 'rgba(16,185,129,0.1)',
            border: '1px solid rgba(16,185,129,0.3)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <KeyOutlined style={{ color: '#10B981', fontSize: 14 }} />
          </div>
          <span style={{ fontWeight: 600, fontSize: 15 }}>设置访问 Token</span>
        </div>
      }
      okText="保存并验证"
      cancelText="取消"
      onOk={handleTokenSave}
      onCancel={() => onOpenChange && onOpenChange(false)}
      closable={false}
      maskClosable={false}
      okButtonProps={{
        style: {
          background: '#10B981', borderColor: '#10B981',
          fontWeight: 600,
        },
      }}
    >
      <div style={{ marginBottom: 12 }}>
        <Text type="secondary" style={{ fontSize: 13 }}>
          输入管理员 Token 以访问 HttpRun 平台。Token 将安全存储在本地。
        </Text>
      </div>
      <Form form={form} onSubmitCapture={handleTokenSave} layout="vertical">
        <Form.Item
          name="token"
          label="Access Token"
          rules={[{ required: true, message: '请输入 Token' }]}
        >
          <Input.Password
            prefix={<LockOutlined style={{ color: '#94A3B8' }} />}
            placeholder="输入您的访问 Token..."
            style={{ fontFamily: "'Fira Code', monospace", height: 40 }}
            autoComplete="off"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default TokenSetting;
