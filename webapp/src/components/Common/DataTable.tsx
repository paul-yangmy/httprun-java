import React from 'react';
import { Table, Empty } from 'antd';
import type { TableProps } from 'antd/es/table';

interface DataTableProps<T> extends TableProps<T> {
  emptyText?: string;
  emptyDescription?: string;
}

/**
 * 数据表格组件
 * 提供统一的表格样式和空状态处理
 */
function DataTable<T extends object>({
  dataSource,
  emptyText = '暂无数据',
  emptyDescription,
  loading = false,
  pagination = { pageSize: 10, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` },
  ...restProps
}: DataTableProps<T>) {
  return (
    <Table<T>
      dataSource={dataSource}
      loading={loading}
      pagination={pagination}
      locale={{
        emptyText: (
          <Empty
            description={
              <span>
                {emptyText}
                {emptyDescription && (
                  <>
                    <br />
                    <span style={{ fontSize: 12, color: '#8c8c8c' }}>{emptyDescription}</span>
                  </>
                )}
              </span>
            }
          />
        ),
      }}
      {...restProps}
    />
  );
}

export default DataTable;
