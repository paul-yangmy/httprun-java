import type { ProLayoutProps } from '@ant-design/pro-components';

/**
 * @name
 */
const Settings: ProLayoutProps & {
  pwa?: boolean;
  logo?: string;
  /** 是否默认启用实时输出（流式模式），优先级：组件可覆盖 */
  useRealtimeOutput?: boolean;
} = {
  navTheme: 'light',
  colorPrimary: '#1677ff',
  layout: 'side',
  contentWidth: 'Fluid',
  fixedHeader: true,
  fixSiderbar: true,
  colorWeak: false,
  title: 'HttpRun',
  pwa: false,
  iconfontUrl: '',
  token: {},
  // 响应式配置
  breakpoint: 'md' as any,
  siderWidth: 208,
  collapsedWidth: 48,
  // 默认不使用实时输出
  useRealtimeOutput: false,
};

export default Settings;
