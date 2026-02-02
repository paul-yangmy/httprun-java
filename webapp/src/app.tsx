import { CodeOutlined, MoonOutlined, SunOutlined } from '@ant-design/icons';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import React, { useEffect, useState } from 'react';
import { Button, Tooltip, ConfigProvider, theme as antdTheme } from 'antd';
import { Footer } from '@/components';
import { validToken } from '@/services/httprun';
import defaultSettings from '../config/defaultSettings';
import { errorConfig } from './requestErrorConfig';
import TokenSetting from '@/components/Token/Setting';

/** 主题存储 key */
const THEME_KEY = 'app_theme_mode';

/** 获取当前主题 */
const getStoredTheme = (): 'light' | 'dark' => {
  const stored = localStorage.getItem(THEME_KEY);
  if (stored === 'dark' || stored === 'light') return stored;
  // 检测系统主题
  if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) {
    return 'dark';
  }
  return 'light';
};

/** 保存主题 */
const setStoredTheme = (mode: 'light' | 'dark') => {
  localStorage.setItem(THEME_KEY, mode);
};

/**
 * @see https://umijs.org/docs/api/runtime-config#getinitialstate
 * */
export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
  themeMode?: 'light' | 'dark';
}> {
  const themeMode = getStoredTheme();
  return {
    settings: {
      ...defaultSettings,
      navTheme: themeMode === 'dark' ? 'realDark' : 'light',
    } as Partial<LayoutSettings>,
    themeMode,
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

// 主题切换按钮组件
const ThemeSwitch: React.FC<{
  themeMode: 'light' | 'dark';
  onChange: (mode: 'light' | 'dark') => void;
}> = ({ themeMode, onChange }) => {
  const isDark = themeMode === 'dark';
  
  return (
    <Tooltip title={isDark ? '切换到亮色模式' : '切换到暗黑模式'}>
      <Button
        type="text"
        icon={isDark ? <SunOutlined /> : <MoonOutlined />}
        onClick={() => {
          const newMode = isDark ? 'light' : 'dark';
          setStoredTheme(newMode);
          // 刷新页面以应用新主题
          window.location.reload();
        }}
        style={{ fontSize: 16 }}
      />
    </Tooltip>
  );
};

// ProLayout 支持的api https://procomponents.ant.design/components/layout
export const layout: RunTimeLayoutConfig = ({ initialState, setInitialState }) => {
  const themeMode = initialState?.themeMode || 'light';
  
  return {
    logo: <CodeOutlined style={{ fontSize: 28, color: '#1677ff' }} />,
    title: 'HttpRun',
    footerRender: () => <Footer />,
    menuHeaderRender: undefined,
    actionsRender: () => [
      <ThemeSwitch
        key="theme"
        themeMode={themeMode}
        onChange={(mode) => {
          setInitialState((prev) => ({
            ...prev,
            themeMode: mode,
            settings: {
              ...prev?.settings,
              navTheme: mode === 'dark' ? 'realDark' : 'light',
            },
          }));
        }}
      />,
    ],
    childrenRender: (children) => {
      const isDark = themeMode === 'dark';
      return (
        <ConfigProvider
          theme={{
            algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
          }}
        >
          <TokenWrapper>{children}</TokenWrapper>
        </ConfigProvider>
      );
    },
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
