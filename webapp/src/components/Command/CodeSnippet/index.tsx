import React, { useState, useMemo } from 'react';
import { Modal, Tabs, Typography, Space, Button, message, Tooltip, Radio, Alert } from 'antd';
import { CopyOutlined, CodeOutlined, CheckOutlined, ApiOutlined, ThunderboltOutlined } from '@ant-design/icons';
import styles from './index.module.less';

const { Text } = Typography;

export interface CodeSnippetProps {
  open: boolean;
  command: HTTPRUN.CommandItem;
  onClose: () => void;
}

type LanguageType = 'curl' | 'python' | 'javascript' | 'http' | 'powershell' | 'wget';
type CodeMode = 'direct' | 'gateway'; // direct: 直接调用API, gateway: 通过HttpRun网关

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

/** 解析 curl 命令，判断是否是 API 调用 */
const isCurlApiCommand = (command: HTTPRUN.CommandItem): boolean => {
  const config = command.commandConfig || command.command;
  const cmdString = (config?.command || '').trim().toLowerCase();
  return cmdString.startsWith('curl') && (cmdString.includes('http://') || cmdString.includes('https://'));
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

/** 替换命令中的参数占位符 */
const replaceCommandParams = (cmdString: string, params: HTTPRUN.ParamDefine[]): string => {
  let result = cmdString;
  params.forEach((param) => {
    const placeholder = `{{.${param.name}}}`;
    const value = generateParamExample(param);
    result = result.replace(new RegExp(placeholder.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), String(value));
  });
  return result;
};

/** 生成请求体（网关模式） */
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

// ==================== 直接模式代码生成器 ====================

const generateDirectCurl = (command: HTTPRUN.CommandItem): string => {
  const config = command.commandConfig || command.command;
  const params = config?.params || [];
  let cmdString = config?.command || '';
  
  // 替换参数占位符
  cmdString = replaceCommandParams(cmdString, params);
  
  return cmdString;
};

const generateDirectPython = (command: HTTPRUN.CommandItem): string => {
  return `# 直接模式下，Python 需要基于原始 curl 命令转换
# 原始命令：
${generateDirectCurl(command)}

# 使用 subprocess 执行：
import subprocess
import json

cmd = """${generateDirectCurl(command).replace(/"/g, '\\"')}"""
result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
print("stdout:", result.stdout)
print("stderr:", result.stderr)`;
};

const generateDirectJavaScript = (command: HTTPRUN.CommandItem): string => {
  return `// 直接模式下，JavaScript 需要基于原始 curl 命令转换
// 原始命令：
// ${generateDirectCurl(command)}

// Node.js 环境使用 child_process：
const { exec } = require('child_process');

const cmd = \`${generateDirectCurl(command)}\`;
exec(cmd, (error, stdout, stderr) => {
  if (error) {
    console.error('Error:', error);
    return;
  }
  console.log('stdout:', stdout);
  console.error('stderr:', stderr);
});`;
};

const generateDirectHttp = (command: HTTPRUN.CommandItem): string => {
  return `# 原始命令：
${generateDirectCurl(command)}

# 注意：HTTP 原始请求需要根据具体的 URL 和参数来构建`;
};

const generateDirectPowerShell = (command: HTTPRUN.CommandItem): string => {
  const config = command.commandConfig || command.command;
  const params = config?.params || [];
  let cmdString = config?.command || '';
  
  // 替换参数占位符
  cmdString = replaceCommandParams(cmdString, params);
  
  // 简单的 curl 到 PowerShell 转换
  if (cmdString.toLowerCase().includes('curl')) {
    return `# PowerShell 中可以使用 curl 别名（Invoke-WebRequest）
# 原始命令：
# ${cmdString}

# PowerShell 等效命令（需根据具体情况调整）：
Invoke-WebRequest -Uri "<URL>" -Method POST -Body @{} -Headers @{}`;
  }
  
  return cmdString;
};

const generateDirectWget = (command: HTTPRUN.CommandItem): string => {
  const curlCmd = generateDirectCurl(command);
  return `# wget 版本（从 curl 转换）
# 原始 curl 命令：
# ${curlCmd}

# wget 等效命令（需根据具体情况调整）：
wget --quiet --method POST --header 'Content-Type: application/json' --body-data '{}' --output-document - '<URL>'`;
};

// ==================== 网关模式代码生成器 ====================

const generateGatewayCurl = (command: HTTPRUN.CommandItem): string => {
  const baseUrl = getBaseUrl();
  const body = generateRequestBody(command);
  const jsonBody = JSON.stringify(body, null, 2);

  return `curl -X POST '${baseUrl}/api/run' \\
  -H 'Content-Type: application/json' \\
  -H 'x-token: <YOUR_TOKEN>' \\
  -d '${jsonBody}'`;
};

const generateGatewayPython = (command: HTTPRUN.CommandItem): string => {
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

const generateGatewayJavaScript = (command: HTTPRUN.CommandItem): string => {
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

const generateGatewayHttp = (command: HTTPRUN.CommandItem): string => {
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

const generateGatewayPowerShell = (command: HTTPRUN.CommandItem): string => {
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

const generateGatewayWget = (command: HTTPRUN.CommandItem): string => {
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

// ==================== 代码生成器映射 ====================

const directGenerators: Record<LanguageType, (command: HTTPRUN.CommandItem) => string> = {
  curl: generateDirectCurl,
  python: generateDirectPython,
  javascript: generateDirectJavaScript,
  http: generateDirectHttp,
  powershell: generateDirectPowerShell,
  wget: generateDirectWget,
};

const gatewayGenerators: Record<LanguageType, (command: HTTPRUN.CommandItem) => string> = {
  curl: generateGatewayCurl,
  python: generateGatewayPython,
  javascript: generateGatewayJavaScript,
  http: generateGatewayHttp,
  powershell: generateGatewayPowerShell,
  wget: generateGatewayWget,
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
  
  const isApiCommand = useMemo(() => isCurlApiCommand(command), [command]);
  const [codeMode, setCodeMode] = useState<CodeMode>(isApiCommand ? 'direct' : 'gateway');

  const code = useMemo(() => {
    const generators = codeMode === 'direct' ? directGenerators : gatewayGenerators;
    const generator = generators[activeLanguage];
    return generator ? generator(command) : '';
  }, [command, activeLanguage, codeMode]);

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
      width={820}
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

        {/* 模式选择（仅当命令是 API 调用时显示） */}
        {isApiCommand && (
          <Alert
            message={
              <Space direction="vertical" size="small" style={{ width: '100%' }}>
                <div>
                  <Text strong>检测到 API 调用命令</Text>
                </div>
                <Radio.Group
                  value={codeMode}
                  onChange={(e) => setCodeMode(e.target.value)}
                  size="small"
                >
                  <Radio.Button value="direct">
                    <ApiOutlined /> 直接调用 API
                  </Radio.Button>
                  <Radio.Button value="gateway">
                    <ThunderboltOutlined /> 通过 HttpRun 网关
                  </Radio.Button>
                </Radio.Group>
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  {codeMode === 'direct' 
                    ? '生成直接调用目标 API 的代码（绕过 HttpRun 网关）'
                    : '生成通过 HttpRun 网关执行命令的代码（需要 Token 认证）'
                  }
                </Text>
              </Space>
            }
            type="info"
            showIcon={false}
            style={{ marginBottom: 12 }}
          />
        )}

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

        {/* Token 提示（仅网关模式） */}
        {codeMode === 'gateway' && (
          <div className={styles.tokenHint}>
            <Text type="warning">
              ⚠️ 请将 <code>&lt;YOUR_TOKEN&gt;</code> 替换为您的实际 Token
            </Text>
          </div>
        )}
      </div>
    </Modal>
  );
};

export default CodeSnippet;
