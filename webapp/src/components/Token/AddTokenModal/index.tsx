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
  TimePicker,
  Checkbox,
  Divider,
  Collapse,
} from 'antd';
import { KeyOutlined, CheckCircleOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { createToken, getCommandList } from '@/services/httprun';
import dayjs from 'dayjs';

const { Text, Paragraph } = Typography;
const { Panel } = Collapse;

// 星期几选项
const WEEKDAY_OPTIONS = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 },
];

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
          remark: value.remark,
        };

        // 处理时间范围限制
        if (value.allowedTimeRange && value.allowedTimeRange.length === 2) {
          req.allowedStartTime = value.allowedTimeRange[0].format('HH:mm');
          req.allowedEndTime = value.allowedTimeRange[1].format('HH:mm');
        }

        // 处理允许的星期几
        if (value.allowedWeekdays && value.allowedWeekdays.length > 0) {
          req.allowedWeekdays = value.allowedWeekdays;
        }

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

  // 快速选择工作日
  const handleSelectWorkdays = () => {
    form.setFieldValue('allowedWeekdays', [1, 2, 3, 4, 5]);
  };

  // 快速选择所有天
  const handleSelectAllDays = () => {
    form.setFieldValue('allowedWeekdays', [1, 2, 3, 4, 5, 6, 7]);
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
      width={600}
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
            tooltip="Token 的有效时长（小时），选择永久则不会过期"
          >
            <Select placeholder="请选择有效时长">
              <Select.Option value={24}>1 天（24小时）</Select.Option>
              <Select.Option value={168}>7 天</Select.Option>
              <Select.Option value={720}>30 天</Select.Option>
              <Select.Option value={2160}>90 天</Select.Option>
              <Select.Option value={8760}>1 年</Select.Option>
              <Select.Option value={-1}>永久</Select.Option>
            </Select>
          </Form.Item>

          <Divider style={{ margin: '16px 0' }} />
          
          <Collapse 
            ghost 
            defaultActiveKey={[]}
            style={{ marginBottom: 16 }}
          >
            <Panel 
              header={
                <Space>
                  <ClockCircleOutlined />
                  <span>时间范围限制（可选）</span>
                </Space>
              } 
              key="timeRestriction"
            >
              <Form.Item
                label="允许时段"
                name="allowedTimeRange"
                tooltip="设置每天允许执行命令的时间段，留空表示不限制"
                labelCol={{ span: 5 }}
                wrapperCol={{ span: 19 }}
              >
                <TimePicker.RangePicker
                  format="HH:mm"
                  placeholder={['开始时间', '结束时间']}
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item
                label="允许星期"
                name="allowedWeekdays"
                tooltip="设置允许执行命令的星期几，不选则不限制"
                labelCol={{ span: 5 }}
                wrapperCol={{ span: 19 }}
              >
                <Checkbox.Group options={WEEKDAY_OPTIONS} />
              </Form.Item>
              <Form.Item
                wrapperCol={{ offset: 5, span: 19 }}
              >
                <Space>
                  <a onClick={handleSelectWorkdays}>选择工作日</a>
                  <a onClick={handleSelectAllDays}>选择全部</a>
                </Space>
              </Form.Item>
              <Alert
                message="时间限制说明"
                description="设置时间限制后，Token 仅在指定的时间段和星期内有效。例如：设置 09:00-18:00 和周一到周五，则该 Token 只能在工作日的工作时间内使用。"
                type="info"
                showIcon
                style={{ marginBottom: 8 }}
              />
            </Panel>
          </Collapse>

          <Form.Item
            label="备注"
            name="remark"
            tooltip="可选的备注信息"
          >
            <Input.TextArea 
              placeholder="请输入备注信息（可选）" 
              rows={2}
              maxLength={500}
              showCount
            />
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
};

export default AddTokenModal;
