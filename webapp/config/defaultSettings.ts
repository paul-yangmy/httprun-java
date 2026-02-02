import type { ProLayoutProps } from '@ant-design/pro-components';

/**
 * @name
 */
const Settings: ProLayoutProps & {
  pwa?: boolean;
  logo?: string;
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
};

export default Settings;
