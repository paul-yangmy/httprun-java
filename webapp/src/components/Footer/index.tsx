import { CodeOutlined, GithubOutlined } from '@ant-design/icons';
import React from 'react';

const Footer: React.FC = () => {
  return (
    <div style={{
      textAlign: 'center',
      padding: '16px 24px',
      borderTop: '1px solid rgba(128,128,128,0.1)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 20,
    }}>
      <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, opacity: 0.55 }}>
        <CodeOutlined style={{ color: '#10B981', fontSize: 13 }} />
        <span style={{ fontFamily: "'Fira Code', monospace", letterSpacing: '0.02em' }}>HttpRun</span>
        <span style={{ opacity: 0.6 }}>· 命令执行管理平台</span>
      </span>
      <a
        href=""
        target="_blank"
        rel="noopener noreferrer"
        style={{ opacity: 0.4, fontSize: 14, color: 'inherit', lineHeight: 1 }}
      >
        <GithubOutlined />
      </a>
    </div>
  );
};

export default Footer;

