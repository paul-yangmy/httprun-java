import { useMemo, useState } from 'react';

/**
 * 搜索过滤 Hook
 * 提供搜索文本管理和数据过滤功能
 */
export function useSearch<T>(
  data: T[],
  filterFn: (item: T, searchText: string) => boolean,
) {
  const [searchText, setSearchText] = useState<string>('');

  const filteredData = useMemo(() => {
    if (!searchText) return data;
    return data.filter((item) => filterFn(item, searchText.toLowerCase()));
  }, [data, searchText, filterFn]);

  return {
    searchText,
    setSearchText,
    filteredData,
  };
}

/**
 * 分页 Hook
 * 提供分页状态管理
 */
export function usePagination(initialPage: number = 1, initialPageSize: number = 10) {
  const [page, setPage] = useState({
    pageIndex: initialPage,
    pageSize: initialPageSize,
  });

  const handlePageChange = (pageIndex: number, pageSize?: number) => {
    setPage({
      pageIndex,
      pageSize: pageSize || page.pageSize,
    });
  };

  return {
    page,
    setPage,
    handlePageChange,
  };
}

/**
 * 列表刷新 Hook
 * 提供列表加载和刷新功能
 */
export function useListRefresh<T>(
  fetchFn: () => Promise<T[]>,
  dependencies: any[] = [],
) {
  const [data, setData] = React.useState<T[]>([]);
  const [loading, setLoading] = React.useState<boolean>(false);

  const refresh = React.useCallback(() => {
    setLoading(true);
    fetchFn()
      .then((result) => {
        setData(result || []);
      })
      .catch((error) => {
        console.error('Fetch error:', error);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [fetchFn, ...dependencies]);

  React.useEffect(() => {
    refresh();
  }, [refresh]);

  return {
    data,
    loading,
    refresh,
  };
}

import React from 'react';
