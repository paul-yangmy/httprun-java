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

  // 加载/验证界面公共背景样式
  const authBg: React.CSSProperties = {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100vh',
    flexDirection: 'column',
    background: 'linear-gradient(135deg, #0a0e1a 0%, #0d1a12 60%, #0a0e1a 100%)',
    gap: 0,
    position: 'relative',
    overflow: 'hidden',
  };

  const gridBg: React.CSSProperties = {
    position: 'absolute',
    inset: 0,
    backgroundImage:
      'linear-gradient(rgba(16,185,129,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(16,185,129,0.04) 1px, transparent 1px)',
    backgroundSize: '40px 40px',
    pointerEvents: 'none',
  };

  // 显示 Token 设置对话框时，同时显示一个友好的等待界面
  if (isValidating && !isTokenValid) {
    return (
      <>
        <TokenSetting
          open={showTokenSetting}
          onOpenChange={setShowTokenSetting}
          onOk={handleTokenSet}
        />
        <div style={authBg}>
          <div style={gridBg} />
          <div style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 20,
            zIndex: 1,
            animation: 'fadeUp 0.4s ease both',
          }}>
            <div style={{
              width: 72, height: 72,
              borderRadius: 18,
              background: 'rgba(16,185,129,0.12)',
              border: '1.5px solid rgba(16,185,129,0.4)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: '0 0 32px rgba(16,185,129,0.2)',
            }}>
              <CodeOutlined style={{ fontSize: 32, color: '#10B981' }} />
            </div>
            <div style={{ textAlign: 'center' }}>
              <div style={{
                fontFamily: "'Fira Code', monospace",
                fontSize: 22, fontWeight: 700, color: '#F8FAFC',
                letterSpacing: '-0.03em', marginBottom: 6,
              }}>HttpRun</div>
              <div style={{
                fontFamily: "'Fira Code', monospace",
                fontSize: 12, color: '#10B981', letterSpacing: '0.08em',
              }}>
                <Spin size="small" style={{ marginRight: 8 }} />
                正在验证身份<span style={{ animation: 'blink 1.2s step-end infinite' }}>...</span>
              </div>
            </div>
          </div>
          <style>{`
            @keyframes fadeUp { from { opacity:0; transform:translateY(16px) } to { opacity:1; transform:translateY(0) } }
            @keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }
          `}</style>
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
        <div style={authBg}>
          <div style={gridBg} />
          <div style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 28,
            zIndex: 1,
            animation: 'fadeUp 0.5s ease both',
          }}>
            {/* Logo icon */}
            <div style={{
              width: 88, height: 88,
              borderRadius: 22,
              background: 'rgba(16,185,129,0.1)',
              border: '1.5px solid rgba(16,185,129,0.5)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: '0 0 48px rgba(16,185,129,0.25), inset 0 0 20px rgba(16,185,129,0.05)',
            }}>
              <CodeOutlined style={{ fontSize: 40, color: '#10B981' }} />
            </div>
            {/* Brand */}
            <div style={{ textAlign: 'center' }}>
              <div style={{
                fontFamily: "'Fira Code', monospace",
                fontSize: 28, fontWeight: 700, color: '#F8FAFC',
                letterSpacing: '-0.04em', marginBottom: 8,
              }}>HttpRun</div>
              <div style={{
                fontSize: 13, color: '#64748B', letterSpacing: '0.04em',
              }}>命令执行管理平台</div>
            </div>
            {/* Terminal prompt card */}
            <div style={{
              background: 'rgba(15,23,42,0.8)',
              border: '1px solid rgba(16,185,129,0.2)',
              borderRadius: 12,
              padding: '20px 28px',
              textAlign: 'center',
              minWidth: 280,
              backdropFilter: 'blur(12px)',
            }}>
              <div style={{
                fontFamily: "'Fira Code', monospace",
                fontSize: 12, color: '#10B981', marginBottom: 16,
                opacity: 0.7,
              }}>$ httprun --auth</div>
              <div style={{ fontSize: 13, color: '#94A3B8', marginBottom: 20 }}>
                请先设置 Token 以访问系统
              </div>
              <Button
                type="primary"
                size="large"
                onClick={() => setShowTokenSetting(true)}
                style={{
                  background: '#10B981',
                  borderColor: '#10B981',
                  fontFamily: "'Fira Code', monospace",
                  fontWeight: 600,
                  letterSpacing: '0.02em',
                  height: 42,
                  paddingInline: 28,
                  boxShadow: '0 4px 20px rgba(16,185,129,0.35)',
                }}
              >
                设置 Token
              </Button>
            </div>
          </div>
          <style>{`
            @keyframes fadeUp { from { opacity:0; transform:translateY(20px) } to { opacity:1; transform:translateY(0) } }
          `}</style>
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
        style={{
          fontSize: 16,
          width: 36,
          height: 36,
          borderRadius: 8,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}
      />
    </Tooltip>
  );
};

// ProLayout 支持的api https://procomponents.ant.design/components/layout
export const layout: RunTimeLayoutConfig = ({ initialState, setInitialState }) => {
  const themeMode = initialState?.themeMode || 'light';
  
  return {
    logo: (
      <div style={{
        width: 32, height: 32,
        borderRadius: 8,
        background: 'rgba(16,185,129,0.15)',
        border: '1px solid rgba(16,185,129,0.4)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        boxShadow: '0 0 12px rgba(16,185,129,0.2)',
      }}>
        <CodeOutlined style={{ fontSize: 16, color: '#10B981' }} />
      </div>
    ),
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
            token: {
              colorPrimary: '#10B981',
              colorLink: '#10B981',
              borderRadius: 8,
              fontFamily: "'Noto Sans SC', AlibabaSans, -apple-system, sans-serif",
            },
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
