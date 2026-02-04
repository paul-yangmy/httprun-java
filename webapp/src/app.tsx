import { CodeOutlined, MoonOutlined, SunOutlined } from '@ant-design/icons';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import React, { useEffect, useState, useCallback } from 'react';
import { Button, Tooltip, ConfigProvider, theme as antdTheme, Spin } from 'antd';
import { Footer } from '@/components';
import { validToken, getCurrentUser } from '@/services/httprun';
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
  currentUser?: HTTPRUN.CurrentUser;
}> {
  const themeMode = getStoredTheme();
  
  // 尝试获取当前用户信息
  let currentUser: HTTPRUN.CurrentUser | undefined;
  try {
    currentUser = await getCurrentUser();
  } catch (error) {
    // Token 无效或未设置，用户信息为空
    currentUser = undefined;
  }
  
  return {
    settings: {
      ...defaultSettings,
      navTheme: themeMode === 'dark' ? 'realDark' : 'light',
    } as Partial<LayoutSettings>,
    themeMode,
    currentUser,
  };
}

// Token 验证包装组件
const TokenWrapper: React.FC<{ 
  children: React.ReactNode;
  onUserChange?: (user: HTTPRUN.CurrentUser | undefined) => void;
}> = ({ children, onUserChange }) => {
  const [showTokenSetting, setShowTokenSetting] = useState(false);
  const [isValidating, setIsValidating] = useState(true);
  const [isTokenValid, setIsTokenValid] = useState(false);
  
  const validateAndFetchUser = useCallback(async () => {
    setIsValidating(true);
    try {
      await validToken();
      // Token 有效，获取用户信息
      const user = await getCurrentUser();
      onUserChange?.(user);
      setIsTokenValid(true);
    } catch {
      // Token 无效或未设置
      setShowTokenSetting(true);
      onUserChange?.(undefined);
      setIsTokenValid(false);
    } finally {
      setIsValidating(false);
    }
  }, [onUserChange]);

  useEffect(() => {
    validateAndFetchUser();
  }, [validateAndFetchUser]);

  const handleTokenSet = useCallback(async () => {
    // Token 设置后重新验证
    setShowTokenSetting(false);
    await validateAndFetchUser();
  }, [validateAndFetchUser]);

  // 显示 Token 设置对话框时，同时显示一个友好的等待界面
  if (isValidating && !isTokenValid) {
    return (
      <>
        <TokenSetting
          open={showTokenSetting}
          onOpenChange={setShowTokenSetting}
          onOk={handleTokenSet}
        />
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '100vh',
          flexDirection: 'column',
          gap: 16
        }}>
          <Spin size="large" />
          <span style={{ color: '#666' }}>正在验证身份...</span>
        </div>
      </>
    );
  }

  // Token 无效时显示设置对话框和提示
  if (!isTokenValid && !isValidating) {
    return (
      <>
        <TokenSetting
          open={showTokenSetting}
          onOpenChange={(open) => {
            // 如果 Token 无效，不允许关闭对话框
            if (!open && !isTokenValid) {
              return;
            }
            setShowTokenSetting(open);
          }}
          onOk={handleTokenSet}
        />
        <div style={{ 
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          height: '100vh',
          flexDirection: 'column',
          gap: 16
        }}>
          <CodeOutlined style={{ fontSize: 64, color: '#1677ff' }} />
          <h2 style={{ margin: 0 }}>HttpRun</h2>
          <p style={{ color: '#666', margin: 0 }}>请先设置 Token 以访问系统</p>
          <Button type="primary" onClick={() => setShowTokenSetting(true)}>
            设置 Token
          </Button>
        </div>
      </>
    );
  }

  return (
    <>
      <TokenSetting
        open={showTokenSetting}
        onOpenChange={setShowTokenSetting}
        onOk={handleTokenSet}
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
          <TokenWrapper
            onUserChange={(user) => {
              setInitialState((prev) => ({
                ...prev,
                currentUser: user,
              }));
            }}
          >
            {children}
          </TokenWrapper>
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
