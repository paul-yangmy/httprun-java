// HttpRun 类型定义

declare namespace HTTPRUN {
  /** 参数定义 */
  type ParamDefine = {
    name: string;
    description: string;
    type: 'string' | 'int' | 'bool';
    defaultValue: string | number | boolean | undefined;
    required: boolean;
  };

  /** 参数 */
  type Param = {
    name: string;
    value: string | number | boolean;
  };

  /** 环境变量 */
  type Environment = {
    name: string;
    value: string;
  };

  /** 命令配置 */
  type CommandConfig = {
    command: string;
    params: ParamDefine[];
    env: Environment[];
  };

  /** 命令状态 */
  type CommandStatus = 0 | 1; // 0: Active, 1: Inactive

  /** 命令项 */
  type CommandItem = {
    id: number;
    commandConfig: CommandConfig;
    /** @deprecated 使用 commandConfig */
    command?: CommandConfig;
    path: string;
    name: string;
    status: CommandStatus;
    description: string;
    created_at: string;
    updated_at: string;
  };

  /** 创建命令响应 */
  type CreateCommandResponse = {
    id: number;
  };

  /** 命令输出响应 */
  type CommandOutputResponse = {
    stdout: string;
    stderr: string;
    error: string;
  };

  /** 访问日志项 */
  type AccessLogItem = {
    id: number;
    token_id: string;
    path: string;
    ip: string;
    request: string;
    response: string;
    created_at: string;
    updated_at: string;
  };

  /** 访问日志列表响应 */
  type AccessLogListResponse = {
    items: AccessLogItem[];
    total: number;
  };

  /** Token */
  type Token = {
    id: number;
    jwtToken: string;
    issuedAt: number;
    expiresAt: number;
    subject: string;
    name: string;
    isAdmin: boolean;
    revoked: boolean;
    createdAt: string;
    updatedAt: string;
  };

  /** Token 列表响应 - 后端直接返回数组 */
  type TokenListResponse = Token[];

  /** 创建 Token 请求 */
  type CreateTokenRequest = {
    name: string;
    commands?: string[];
    isAdmin?: boolean;
    expiresIn?: number;
    remark?: string;
  };

  /** 创建 Token 响应 - 后端返回完整 Token 对象 */
  type CreateTokenResponse = Token;

  /** 当前用户信息 */
  type CurrentUser = {
    name: string;
    isAdmin: boolean;
    token: string;
  };
}
