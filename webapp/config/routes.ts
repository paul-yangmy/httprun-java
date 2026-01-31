/**
 * @name umi 的路由配置
 * @description 只支持 path,component,routes,redirect,wrappers,name,icon 的配置
 * @doc https://umijs.org/docs/guides/routes
 */
export default [
  {
    path: '/',
    name: '命令列表',
    icon: 'thunderbolt',
    component: './Command/UserList',
  },
  {
    path: '/admin',
    name: '管理',
    icon: 'crown',
    routes: [
      {
        path: '/admin',
        redirect: '/admin/command',
      },
      {
        path: '/admin/command',
        name: '命令管理',
        icon: 'code',
        component: './Admin/Command',
      },
      {
        path: '/admin/token',
        name: 'Token管理',
        icon: 'key',
        component: './Admin/Token',
      },
      {
        path: '/admin/accesslog',
        name: '访问日志',
        icon: 'fileText',
        component: './Admin/AccessLog',
      },
    ],
  },
  {
    component: '404',
    layout: false,
    path: './*',
  },
];
