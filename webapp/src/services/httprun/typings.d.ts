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

  /** 远程执行配置（SSH 模式） */
  type RemoteConfig = {
    /** 主机地址，默认 localhost 表示本机 */
    host?: string;
    /** SSH 端口，默认 22 */
    port?: number;
    /** SSH 用户名 */
    username?: string;
    /** SSH 密码（与 privateKey 二选一） */
    password?: string;
    /** SSH 私钥（与 password 二选一） */
    privateKey?: string;
  };

  /** 执行模式 */
  type ExecutionMode = 'LOCAL' | 'SSH' | 'AGENT';

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
    /** 执行模式 */
    executionMode?: ExecutionMode;
    /** 远程执行配置 */
    remoteConfig?: RemoteConfig;
    /** 危险等级：0=安全, 1=警告, 2=高危 */
    dangerLevel?: number;
    /** 危险警告信息 */
    dangerWarning?: string;
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
    tokenId: string;
    path: string;
    ip: string;
    method: string;
    request: string;
    response: string;
    statusCode: number;
    duration: number;
    // 审计增强字段
    userAgent: string;
    referer: string;
    source: string;      // WEB / API / CLI
    forwardedFor: string;
    requestId: string;
    commandName: string;
    createdAt: string;
  };

  /** 访问日志列表响应 (Spring Data Page 格式) */
  type AccessLogListResponse = {
    content: AccessLogItem[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
  };

  /** 执行历史响应 (Spring Data Page 格式) */
  type ExecutionHistoryResponse = {
    content: AccessLogItem[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
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
    /** 允许执行的开始时间（格式：HH:mm） */
    allowedStartTime?: string;
    /** 允许执行的结束时间（格式：HH:mm） */
    allowedEndTime?: string;
    /** 允许执行的星期几（逗号分隔，如 "1,2,3,4,5"） */
    allowedWeekdays?: string;
    /** 备注信息 */
    remark?: string;
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
    /** 允许执行的开始时间（格式：HH:mm） */
    allowedStartTime?: string;
    /** 允许执行的结束时间（格式：HH:mm） */
    allowedEndTime?: string;
    /** 允许执行的星期几（1=周一, ..., 7=周日） */
    allowedWeekdays?: number[];
    remark?: string;
  };

  /** 创建 Token 响应 - 后端返回完整 Token 对象 */
  type CreateTokenResponse = Token;

  /** 撤销 Token 响应 */
  type RevokeTokenResponse = {
    /** 操作是否成功 */
    success: boolean;
    /** 提示消息 */
    message: string;
    /** 是否生成了新的管理员 Token */
    newAdminTokenGenerated: boolean;
    /** 新生成的管理员 Token ID（仅当撤销管理员 Token 时返回） */
    newTokenId?: number;
    /** 新生成的管理员 Token 名称 */
    newTokenName?: string;
    /** 新生成的 JWT Token（仅当撤销管理员 Token 时返回） */
    newJwtToken?: string;
    /** 警告信息 */
    warning?: string;
  };

  /** 当前用户信息 */
  type CurrentUser = {
    name: string;
    isAdmin: boolean;
    token: string;
  };
}
