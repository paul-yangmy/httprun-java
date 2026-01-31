import React, { useCallback, useEffect, useState } from 'react';
import {
  Form,
  Modal,
  Input,
  Select,
  message,
  Typography,
  Space,
  Alert,
} from 'antd';
import { KeyOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { createToken, getCommandList } from '@/services/httprun';

const { Text, Paragraph } = Typography;

export interface AddTokenModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onChange?: (res?: HTTPRUN.CreateTokenResponse) => void;
}

const AddTokenModal: React.FC<AddTokenModalProps> = ({
  open,
  onChange,
  onOpenChange,
}) => {
  const [commandList, setCommandList] = useState<HTTPRUN.CommandItem[]>([]);
  const [commandListLoading, setCommandListLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [createdToken, setCreatedToken] = useState<string | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      setCommandListLoading(true);
      setCreatedToken(null);
      getCommandList()
        .then((data) => {
          setCommandListLoading(false);
          setCommandList(data || []);
        })
        .catch(() => {
          setCommandListLoading(false);
        });
    }
  }, [open]);

  const handleSubmit = useCallback(() => {
    form
      .validateFields()
      .then(() => {
        setLoading(true);
        const value = form.getFieldsValue();
        // 构造符合后端 CreateTokenRequest 的请求
        const req: HTTPRUN.CreateTokenRequest = {
          name: value.name,
          commands: value.subject || [],
          isAdmin: value.isAdmin || false,
          expiresIn: value.expiresIn || 24,
        };
        createToken(req)
          .then((res) => {
            setLoading(false);
            // 后端返回完整 Token 对象，jwtToken 字段包含 token 值
            setCreatedToken(res.jwtToken);
            message.success('Token 创建成功！');
            if (onChange) {
              onChange(res);
            }
          })
          .catch(() => {
            setLoading(false);
            message.error('创建失败');
          });
      })
      .catch(() => {
        message.warning('请填写完整信息');
      });
  }, [form, onChange]);

  const handleClose = () => {
    form.resetFields();
    setCreatedToken(null);
    if (onOpenChange) {
      onOpenChange(false);
    }
  };

  return (
    <Modal
      title={
        <Space>
          <KeyOutlined style={{ color: '#1677ff' }} />
          <span>{createdToken ? 'Token 创建成功' : '添加 Token'}</span>
        </Space>
      }
      onCancel={handleClose}
      open={open}
      okText={createdToken ? '完成' : '创建'}
      cancelText="取消"
      okButtonProps={{
        onClick: createdToken ? handleClose : handleSubmit,
        loading: loading,
        icon: createdToken ? <CheckCircleOutlined /> : undefined,
      }}
      cancelButtonProps={{ style: createdToken ? { display: 'none' } : {} }}
      width={550}
    >
      {createdToken ? (
        <div style={{ padding: '16px 0' }}>
          <Alert
            message="Token 创建成功"
            description="请妥善保管以下 Token，关闭后将无法再次查看完整内容。"
            type="success"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <div
            style={{
              background: '#f5f5f5',
              padding: 16,
              borderRadius: 8,
              border: '1px solid #d9d9d9',
            }}
          >
            <Text type="secondary" style={{ marginBottom: 8, display: 'block' }}>
              Token 值
            </Text>
            <Paragraph
              copyable={{
                text: createdToken,
                tooltips: ['复制 Token', '已复制！'],
              }}
              style={{
                marginBottom: 0,
                wordBreak: 'break-all',
                fontFamily: 'monospace',
                fontSize: 12,
              }}
            >
              {createdToken}
            </Paragraph>
          </div>
        </div>
      ) : (
        <Form
          labelCol={{ span: 5 }}
          wrapperCol={{ span: 19 }}
          form={form}
          style={{ marginTop: 24 }}
          initialValues={{ expiresIn: 24 }}
        >
          <Form.Item
            label="名称"
            name="name"
            rules={[{ required: true, message: '请输入 Token 名称' }]}
            tooltip="用于标识 Token 的用途或所属用户"
          >
            <Input placeholder="请输入 Token 名称" />
          </Form.Item>
          <Form.Item
            label="授权命令"
            name="subject"
            tooltip="选择此 Token 可以执行的命令，不选则授权所有命令"
          >
            <Select
              mode="multiple"
              loading={commandListLoading}
              placeholder="请选择授权命令（不选则授权所有命令）"
              optionFilterProp="children"
              showSearch
              allowClear
            >
              {commandList.map((it) => (
                <Select.Option key={it.name} value={it.name}>
                  {it.name}
                  {it.description && (
                    <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                      - {it.description}
                    </Text>
                  )}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            label="有效时长"
            name="expiresIn"
            rules={[{ required: true, message: '请选择有效时长' }]}
            tooltip="Token 的有效时长（小时）"
          >
            <Select placeholder="请选择有效时长">
              <Select.Option value={24}>1 天（24小时）</Select.Option>
              <Select.Option value={168}>7 天</Select.Option>
              <Select.Option value={720}>30 天</Select.Option>
              <Select.Option value={2160}>90 天</Select.Option>
              <Select.Option value={8760}>1 年</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
};

export default AddTokenModal;
