import React, { useState, useMemo } from 'react';
import { Modal, Tabs, Typography, Select, Space, Button, message, Tooltip } from 'antd';
import { CopyOutlined, CodeOutlined, CheckOutlined } from '@ant-design/icons';
import styles from './index.module.less';

const { Text } = Typography;

export interface CodeSnippetProps {
  open: boolean;
  command: HTTPRUN.CommandItem;
  onClose: () => void;
}

type LanguageType = 'curl' | 'python' | 'javascript' | 'http' | 'powershell' | 'wget';

interface LanguageOption {
  key: LanguageType;
  label: string;
  icon: string;
}

const languageOptions: LanguageOption[] = [
  { key: 'curl', label: 'cURL', icon: '🔧' },
  { key: 'python', label: 'Python', icon: '🐍' },
  { key: 'javascript', label: 'JavaScript', icon: '📜' },
  { key: 'http', label: 'HTTP', icon: '🌐' },
  { key: 'powershell', label: 'PowerShell', icon: '💻' },
  { key: 'wget', label: 'wget', icon: '📥' },
];

/** 获取 API 基础 URL */
const getBaseUrl = (): string => {
  return window.location.origin;
};

/** 生成参数示例值 */
const generateParamExample = (param: HTTPRUN.ParamDefine): string | number | boolean => {
  if (param.defaultValue !== undefined && param.defaultValue !== '') {
    return param.defaultValue;
  }
  switch (param.type) {
    case 'int':
      return 0;
    case 'bool':
      return false;
    case 'string':
    default:
      return `<${param.name}>`;
  }
};

/** 生成请求体 */
const generateRequestBody = (command: HTTPRUN.CommandItem): object => {
  const config = command.commandConfig || command.command;
  const params = config?.params || [];
  const env = config?.env || [];

  return {
    name: command.name,
    params: params.map((p) => ({
      name: p.name,
      value: generateParamExample(p),
    })),
    env: env.map((e) => ({
      name: e.name,
      value: e.value || `<${e.name}>`,
    })),
  };
};

/** 生成 cURL 代码 */
const generateCurl = (command: HTTPRUN.CommandItem): string => {
  const baseUrl = getBaseUrl();
  const body = generateRequestBody(command);
  const jsonBody = JSON.stringify(body, null, 2);

  return `curl -X POST '${baseUrl}/api/run' \\
  -H 'Content-Type: application/json' \\
  -H 'x-token: <YOUR_TOKEN>' \\
  -d '${jsonBody}'`;
};

/** 生成 Python 代码 */
const generatePython = (command: HTTPRUN.CommandItem): string => {
  const baseUrl = getBaseUrl();
  const body = generateRequestBody(command);
  const jsonBody = JSON.stringify(body, null, 4);

  return `import requests

url = "${baseUrl}/api/run"
headers = {
    "Content-Type": "application/json",
    "x-token": "<YOUR_TOKEN>"
}
data = ${jsonBody}

response = requests.post(url, json=data, headers=headers)

if response.status_code == 200:
    result = response.json()
    print("stdout:", result.get("stdout", ""))
    print("stderr:", result.get("stderr", ""))
else:
    print(f"Error: {response.status_code}")
    print(response.text)`;
};

/** 生成 JavaScript (Fetch) 代码 */
const generateJavaScript = (command: HTTPRUN.CommandItem): string => {
  const baseUrl = getBaseUrl();
  const body = generateRequestBody(command);
  const jsonBody = JSON.stringify(body, null, 2);

  return `const response = await fetch('${baseUrl}/api/run', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'x-token': '<YOUR_TOKEN>'
  },
  body: JSON.stringify(${jsonBody})
});

if (response.ok) {
  const result = await response.json();
  console.log('stdout:', result.stdout);
  console.log('stderr:', result.stderr);
} else {
  console.error('Error:', response.status, await response.text());
}`;
};

/** 生成 HTTP 原始请求 */
const generateHttp = (command: HTTPRUN.CommandItem): string => {
  const baseUrl = getBaseUrl();
  const body = generateRequestBody(command);
  const jsonBody = JSON.stringify(body, null, 2);
  const url = new URL('/api/run', baseUrl);

  return `POST ${url.pathname} HTTP/1.1
Host: ${url.host}
Content-Type: application/json
x-token: <YOUR_TOKEN>

${jsonBody}`;
};

/** 生成 PowerShell 代码 */
const generatePowerShell = (command: HTTPRUN.CommandItem): string => {
  const baseUrl = getBaseUrl();
  const body = generateRequestBody(command);
  const jsonBody = JSON.stringify(body, null, 2);

  return `$headers = @{
    "Content-Type" = "application/json"
    "x-token" = "<YOUR_TOKEN>"
}

$body = @'
${jsonBody}
'@

$response = Invoke-RestMethod -Uri "${baseUrl}/api/run" \`
    -Method POST \`
    -Headers $headers \`
    -Body $body

Write-Host "stdout: $($response.stdout)"
Write-Host "stderr: $($response.stderr)"`;
};

/** 生成 wget 代码 */
const generateWget = (command: HTTPRUN.CommandItem): string => {
  const baseUrl = getBaseUrl();
  const body = generateRequestBody(command);
  const jsonBody = JSON.stringify(body);

  return `wget --quiet \\
  --method POST \\
  --header 'Content-Type: application/json' \\
  --header 'x-token: <YOUR_TOKEN>' \\
  --body-data '${jsonBody}' \\
  --output-document - \\
  '${baseUrl}/api/run'`;
};

/** 代码生成器映射 */
const codeGenerators: Record<LanguageType, (command: HTTPRUN.CommandItem) => string> = {
  curl: generateCurl,
  python: generatePython,
  javascript: generateJavaScript,
  http: generateHttp,
  powershell: generatePowerShell,
  wget: generateWget,
};

/** 获取语言的语法高亮类名 */
const getLanguageClass = (lang: LanguageType): string => {
  const mapping: Record<LanguageType, string> = {
    curl: 'bash',
    python: 'python',
    javascript: 'javascript',
    http: 'http',
    powershell: 'powershell',
    wget: 'bash',
  };
  return mapping[lang] || 'plaintext';
};

const CodeSnippet: React.FC<CodeSnippetProps> = ({ open, command, onClose }) => {
  const [activeLanguage, setActiveLanguage] = useState<LanguageType>('curl');
  const [copied, setCopied] = useState(false);

  const code = useMemo(() => {
    const generator = codeGenerators[activeLanguage];
    return generator ? generator(command) : '';
  }, [command, activeLanguage]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      message.success('代码已复制到剪贴板');
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      message.error('复制失败，请手动复制');
    }
  };

  const config = command.commandConfig || command.command;
  const params = config?.params || [];

  const tabItems = languageOptions.map((lang) => ({
    key: lang.key,
    label: (
      <span>
        {lang.icon} {lang.label}
      </span>
    ),
  }));

  return (
    <Modal
      title={
        <Space>
          <CodeOutlined />
          <span>代码片段 - {command.name}</span>
        </Space>
      }
      open={open}
      onCancel={onClose}
      footer={null}
      width={720}
      className={styles.codeSnippetModal}
    >
      <div className={styles.container}>
        {/* 命令信息 */}
        <div className={styles.commandInfo}>
          <Text type="secondary">命令: </Text>
          <Text strong>{command.name}</Text>
          {command.description && (
            <>
              <Text type="secondary" style={{ marginLeft: 16 }}>描述: </Text>
              <Text>{command.description}</Text>
            </>
          )}
        </div>

        {/* 参数提示 */}
        {params.length > 0 && (
          <div className={styles.paramsHint}>
            <Text type="secondary">
              参数说明: 请将代码中的占位符替换为实际值。必填参数：
              {params.filter(p => p.required).map(p => p.name).join(', ') || '无'}
            </Text>
          </div>
        )}

        {/* 语言切换 */}
        <Tabs
          activeKey={activeLanguage}
          onChange={(key) => setActiveLanguage(key as LanguageType)}
          items={tabItems}
          tabBarExtraContent={
            <Tooltip title={copied ? '已复制' : '复制代码'}>
              <Button
                type="text"
                icon={copied ? <CheckOutlined style={{ color: '#52c41a' }} /> : <CopyOutlined />}
                onClick={handleCopy}
              >
                {copied ? '已复制' : '复制'}
              </Button>
            </Tooltip>
          }
        />

        {/* 代码展示 */}
        <div className={styles.codeWrapper}>
          <pre className={`${styles.codeBlock} language-${getLanguageClass(activeLanguage)}`}>
            <code>{code}</code>
          </pre>
        </div>

        {/* Token 提示 */}
        <div className={styles.tokenHint}>
          <Text type="warning">
            ⚠️ 请将 <code>&lt;YOUR_TOKEN&gt;</code> 替换为您的实际 Token
          </Text>
        </div>
      </div>
    </Modal>
  );
};

export default CodeSnippet;
