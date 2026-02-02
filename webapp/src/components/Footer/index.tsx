import { CodeOutlined, GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import React from 'react';

const Footer: React.FC = () => {
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      copyright="HttpRun - 命令执行管理平台"
      links={[
        {
          key: 'httprun',
          title: (
            <>
              <CodeOutlined /> HttpRun
            </>
          ),
          href: '/',
          blankTarget: false,
        },
        {
          key: 'github',
          title: <GithubOutlined />,
          href: 'https://github.com/paul-yangmy/httprun-java',
          blankTarget: true,
        },
      ]}
    />
  );
};

export default Footer;

