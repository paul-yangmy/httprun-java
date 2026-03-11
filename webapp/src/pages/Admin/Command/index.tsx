import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Button,
  Card,
  Dropdown,
  Drawer,
  Table,
  Tag,
  Space,
  Input,
  Typography,
  Tooltip,
  Empty,
  message,
  Modal,
  Radio,
  Upload,
  Result,
  Descriptions,
} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  EllipsisOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  ExclamationCircleOutlined,
  WarningOutlined,
  CopyOutlined,
  ExportOutlined,
  ImportOutlined,
  InboxOutlined,
  HistoryOutlined,
} from '@ant-design/icons';
import { Switch } from 'antd';
import { request } from '@umijs/max';
import type { ColumnsType } from 'antd/es/table';
import { PageContainer } from '@ant-design/pro-components';
import { getCommandList, deleteCommand, exportCommands, importCommands, getCommandVersions, rollbackCommandVersion } from '@/services/httprun';
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
  const [copyCommand, setCopyCommand] = useState<HTTPRUN.CommandItem | null>(null);

  // 导入相关
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [importMode, setImportMode] = useState<'rename' | 'skip' | 'overwrite'>('rename');
  const [importResult, setImportResult] = useState<HTTPRUN.CommandImportResult | null>(null);
  const [importLoading, setImportLoading] = useState(false);
  const importFileRef = useRef<HTTPRUN.CommandItem[] | null>(null);

  // 版本历史
  const [versionHistoryVisible, setVersionHistoryVisible] = useState(false);
  const [versionHistoryName, setVersionHistoryName] = useState<string>('');
  const [versionList, setVersionList] = useState<HTTPRUN.CommandVersionItem[]>([]);
  const [versionLoading, setVersionLoading] = useState(false);

  const handleOpenVersionHistory = async (name: string) => {
    setVersionHistoryName(name);
    setVersionHistoryVisible(true);
    setVersionLoading(true);
    try {
      const data = await getCommandVersions(name);
      setVersionList(data || []);
    } catch {
      message.error('获取版本历史失败');
    } finally {
      setVersionLoading(false);
    }
  };

  const handleRollback = (name: string, versionId: number, version: number) => {
    Modal.confirm({
      title: `确认回滚至版本 V${version}？`,
      content: '回滚后当前命令配置将被该版本覆盖，且会自动保存一个当前版本的快照。',
      okText: '确认回滚',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await rollbackCommandVersion(name, versionId);
          message.success(`已成功回滚至版本 V${version}`);
          setVersionHistoryVisible(false);
          refresh();
        } catch {
          message.error('回滚失败');
        }
      },
    });
  };

  // 检查命令名是否已存在
  const isNameDuplicate = (name: string) => items.some(item => item.name === name);

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

  // 导出全部命令为 JSON 文件
  const handleExport = useCallback(async () => {
    try {
      const data = await exportCommands();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `httprun-commands-${new Date().toISOString().slice(0, 10)}.json`;
      a.click();
      URL.revokeObjectURL(url);
      message.success(`已导出 ${data.length} 条命令`);
    } catch {
      message.error('导出失败');
    }
  }, []);

  // 读取上传的 JSON 文件
  const handleFileRead = (file: File): boolean => {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const parsed = JSON.parse(e.target?.result as string);
        const commands = Array.isArray(parsed) ? parsed : [parsed];
        importFileRef.current = commands;
        setImportResult(null);
        message.success(`已解析 ${commands.length} 条命令，点击「确认导入」开始导入`);
      } catch {
        message.error('JSON 格式解析失败，请检查文件内容');
      }
    };
    reader.readAsText(file);
    return false; // 阻止自动上传
  };

  // 执行导入
  const handleImportConfirm = async () => {
    if (!importFileRef.current) {
      message.warning('请先选择要导入的 JSON 文件');
      return;
    }
    setImportLoading(true);
    try {
      const result = await importCommands(importFileRef.current, importMode);
      setImportResult(result);
      refresh();
    } catch {
      message.error('导入失败，请检查文件格式');
    } finally {
      setImportLoading(false);
    }
  };

  const filteredItems = useMemo(() => {
    if (!searchText) return items;
    const lowerSearch = searchText.toLowerCase();
    return items.filter(
      (item) =>
        item.name.toLowerCase().includes(lowerSearch) ||
        item.description.toLowerCase().includes(lowerSearch),
    );
  }, [items, searchText]);

  // 切换命令状态（后端枚举为 ACTIVE / DISABLED）
  const handleStatusChange = async (record: HTTPRUN.CommandItem, checked: boolean) => {
    const status = checked ? 'ACTIVE' : 'DISABLED';
    try {
      await request('/api/admin/commands', {
        method: 'PUT',
        headers: { 'x-token': localStorage.getItem('token') || '', 'Content-Type': 'application/json' },
        data: { commands: [record.name], status },
      });
      message.success(`命令“${record.name}”已${checked ? '启用' : '停用'}`);
      refresh();
    } catch (e) {
      message.error('状态切换失败');
    }
  };

  const columns: ColumnsType<HTTPRUN.CommandItem> = [
    {
      title: '命令名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
      ellipsis: true,
      render: (name: string, record) => (
        <Space>
          <Text strong style={{ color: '#1677ff' }}>
            {name}
          </Text>
          {record.dangerLevel !== undefined && record.dangerLevel >= 2 && (
            <Tag color="error" icon={<ExclamationCircleOutlined />}>
              高危
            </Tag>
          )}
          {record.dangerLevel === 1 && (
            <Tag color="warning" icon={<WarningOutlined />}>
              警告
            </Tag>
          )}
        </Space>
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
      width: 180,
      ellipsis: true,
      responsive: ['lg'] as any,
      render: (path: string) => (
        <Text code copyable={{ text: path }}>
          {path}
        </Text>
      ),
    },
    {
      title: '参数数量',
      key: 'params',
      width: 90,
      align: 'center',
      responsive: ['md'] as any,
      render: (_, record) => {
        const cfg = record.commandConfig || record.command;
        return <Tag color="blue">{cfg?.params?.length || 0} 个</Tag>;
      },
    },
    {
      title: '执行位置',
      key: 'executionMode',
      width: 100,
      align: 'center',
      responsive: ['md'] as any,
      render: (_, record) => {
        const isRemote = record.executionMode === 'SSH';
        const host = record.remoteConfig?.host || 'localhost';
        return (
          <Tooltip title={isRemote ? `SSH: ${host}` : '本机执行'}>
            <Tag color={isRemote ? 'orange' : 'green'}>
              {isRemote ? 'SSH' : '本机'}
            </Tag>
          </Tooltip>
        );
      },
    },
    {
      title: '分组',
      key: 'groupName',
      width: 110,
      responsive: ['lg'] as any,
      render: (_: any, record: HTTPRUN.CommandItem) => {
        const group = record.groupName;
        return group ? <Tag color="purple">{group}</Tag> : <span style={{ color: '#bbb' }}>-</span>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      align: 'center',
      render: (_: any, record: HTTPRUN.CommandItem) => (
        <Switch
          checked={record.status === 0 || record.status === 'ACTIVE'}
          checkedChildren="启用"
          unCheckedChildren="停用"
          onChange={(checked) => handleStatusChange(record, checked)}
        />
      ),
    },
    {
      title: '版本',
      key: 'version',
      width: 65,
      align: 'center' as any,
      render: (_: any, record: HTTPRUN.CommandItem) => (
        <Button
          type="link"
          size="small"
          icon={<HistoryOutlined />}
          onClick={() => handleOpenVersionHistory(record.name)}
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right' as any,
      render: (_, record) => (
        <Space size="small" wrap>
          <Button
            type="primary"
            size="small"
            icon={<PlayCircleOutlined />}
            onClick={() => setCurrentCommand(record)}
          >
            运行
          </Button>
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                {
                  key: 'edit',
                  label: '编辑',
                  icon: <EditOutlined />,
                  onClick: () => setEditCommand(record),
                },
                {
                  key: 'copy',
                  label: '复制',
                  icon: <CopyOutlined />,
                  onClick: () => setCopyCommand(record),
                },
                {
                  key: 'export',
                  label: '导出',
                  icon: <ExportOutlined />,
                  onClick: async () => {
                    try {
                      const data = await exportCommands([record.name]);
                      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
                      const url = URL.createObjectURL(blob);
                      const a = document.createElement('a');
                      a.href = url;
                      a.download = `${record.name}.json`;
                      a.click();
                      URL.revokeObjectURL(url);
                      message.success(`已导出命令「${record.name}」`);
                    } catch {
                      message.error('导出失败');
                    }
                  },
                },
                { type: 'divider' as const },
                {
                  key: 'delete',
                  label: '确认删除',
                  danger: true,
                  icon: <DeleteOutlined />,
                  onClick: () => handleDelete(record.name),
                },
              ],
            }}
          >
            <Button size="small" icon={<EllipsisOutlined />}>
              更多
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
      {/* 复制命令弹窗 */}
      {copyCommand && (
        <CommandEditor
          open
          command={{
            ...copyCommand,
            name: copyCommand.name + '-copy',
          }}
          onClose={() => setCopyCommand(null)}
          onChange={() => {
            setCopyCommand(null);
            refresh();
          }}
          // 复制模式下校验重名
          checkNameDuplicate={isNameDuplicate}
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
          <Space wrap>
            <Input
              placeholder="搜索命令"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ width: 180, minWidth: 120 }}
              allowClear
            />
            <Button icon={<ReloadOutlined />} onClick={refresh}>
              刷新
            </Button>
            <Button icon={<ExportOutlined />} onClick={handleExport}>
              导出
            </Button>
            <Button
              icon={<ImportOutlined />}
              onClick={() => { setImportResult(null); importFileRef.current = null; setImportModalOpen(true); }}
            >
              导入
            </Button>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setEditCommand({})}
            >
              添加
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
          scroll={{ x: 900 }}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条记录`,
            responsive: true,
          }}
          locale={{
            emptyText: <Empty description="暂无命令" />,
          }}
        />
      </Card>

      {/* 导入命令弹窗 */}
      <Modal
        title="导入命令"
        open={importModalOpen}
        onCancel={() => { setImportModalOpen(false); setImportResult(null); }}
        footer={
          importResult ? (
            <Button onClick={() => { setImportModalOpen(false); setImportResult(null); }}>关闭</Button>
          ) : (
            <Space>
              <Button onClick={() => setImportModalOpen(false)}>取消</Button>
              <Button type="primary" loading={importLoading} onClick={handleImportConfirm}>
                确认导入
              </Button>
            </Space>
          )
        }
        width={540}
      >
        {importResult ? (
          <Result
            status={importResult.failed > 0 ? 'warning' : 'success'}
            title="导入完成"
            subTitle={
              <Descriptions column={2} size="small" style={{ marginTop: 12 }}>
                <Descriptions.Item label="新建">{importResult.created}</Descriptions.Item>
                <Descriptions.Item label="重命名">{importResult.renamed}</Descriptions.Item>
                <Descriptions.Item label="覆盖">{importResult.overwritten}</Descriptions.Item>
                <Descriptions.Item label="跳过">{importResult.skipped}</Descriptions.Item>
                <Descriptions.Item label="失败">{importResult.failed}</Descriptions.Item>
              </Descriptions>
            }
            extra={
              importResult.errors.length > 0 && (
                <div style={{ textAlign: 'left', maxHeight: 120, overflow: 'auto' }}>
                  {importResult.errors.map((e, i) => (
                    <div key={i} style={{ color: '#ff4d4f', fontSize: 12 }}>{e}</div>
                  ))}
                </div>
              )
            }
          />
        ) : (
          <Space direction="vertical" style={{ width: '100%' }} size="middle">
            <Upload.Dragger
              accept=".json"
              beforeUpload={handleFileRead}
              showUploadList={false}
              maxCount={1}
            >
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">点击或拖拽 JSON 文件到此区域</p>
              <p className="ant-upload-hint">支持由「导出」功能生成的 JSON 格式，SSH 密码需在导入后重新配置</p>
            </Upload.Dragger>
            <div>
              <div style={{ marginBottom: 8, color: 'rgba(0,0,0,0.65)' }}>遇到同名命令时：</div>
              <Radio.Group value={importMode} onChange={e => setImportMode(e.target.value)}>
                <Radio value="rename">自动重命名（同名命令添加 -copy 后缀）</Radio>
                <Radio value="skip">跳过（保留已有命令不变）</Radio>
                <Radio value="overwrite">覆盖（用导入的命令覆盖同名命令）</Radio>
              </Radio.Group>
            </div>
          </Space>
        )}
      </Modal>
      {/* 版本历史抖帘 */}
      <Drawer
        title={`版本历史 — ${versionHistoryName}`}
        open={versionHistoryVisible}
        onClose={() => setVersionHistoryVisible(false)}
        width={640}
        destroyOnClose
      >
        <Table<HTTPRUN.CommandVersionItem>
          dataSource={versionList}
          rowKey="id"
          loading={versionLoading}
          size="small"
          pagination={{ pageSize: 10, showTotal: (total) => `共 ${total} 个版本` }}
          locale={{ emptyText: <Empty description="暂无版本记录" /> }}
          columns={[
            {
              title: '版本',
              dataIndex: 'version',
              width: 70,
              render: (v: number) => <Tag color="blue">V{v}</Tag>,
            },
            {
              title: '变更时间',
              dataIndex: 'changedAt',
              width: 180,
              render: (t: string) => t ? new Date(t).toLocaleString('zh-CN') : '-',
            },
            {
              title: '变更说明',
              dataIndex: 'changeNote',
              ellipsis: true,
              render: (note: string) => note || <span style={{ color: '#bbb' }}>-</span>,
            },
            {
              title: '操作',
              key: 'action',
              width: 110,
              render: (_: any, ver: HTTPRUN.CommandVersionItem) => (
                <Button
                  size="small"
                  danger
                  onClick={() => handleRollback(versionHistoryName, ver.id, ver.version)}
                >
                  回滚至此版本
                </Button>
              ),
            },
          ]}
        />
      </Drawer>
    </PageContainer>
  );
};

export default AdminCommand;
