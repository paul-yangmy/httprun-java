import { CodeOutlined } from '@ant-design/icons';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import React, { useEffect, useState } from 'react';
import { Footer } from '@/components';
import { validToken } from '@/services/httprun';
import defaultSettings from '../config/defaultSettings';
import { errorConfig } from './requestErrorConfig';
import TokenSetting from '@/components/Token/Setting';

/**
 * @see https://umijs.org/docs/api/runtime-config#getinitialstate
 * */
export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
}> {
  return {
    settings: defaultSettings as Partial<LayoutSettings>,
  };
}

// Token 验证包装组件
const TokenWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [showTokenSetting, setShowTokenSetting] = useState(false);
  
  useEffect(() => {
    validToken().catch(() => {
      setShowTokenSetting(true);
    });
  }, []);

  return (
    <>
      <TokenSetting
        open={showTokenSetting}
        onOpenChange={setShowTokenSetting}
        onOk={() => window.location.reload()}
      />
      {children}
    </>
  );
};

// ProLayout 支持的api https://procomponents.ant.design/components/layout
export const layout: RunTimeLayoutConfig = ({ initialState }) => {
  return {
    logo: <CodeOutlined style={{ fontSize: 28, color: '#1677ff' }} />,
    title: 'HttpRun',
    footerRender: () => <Footer />,
    menuHeaderRender: undefined,
    childrenRender: (children) => <TokenWrapper>{children}</TokenWrapper>,
    ...initialState?.settings,
  };
};

/**
 * @name request 配置，可以配置错误处理
 * 它基于 axios 和 ahooks 的 useRequest 提供了一套统一的网络请求和错误处理方案。
 * @doc https://umijs.org/docs/max/request#配置
 */
export const request: RequestConfig = {
  ...errorConfig,
};
