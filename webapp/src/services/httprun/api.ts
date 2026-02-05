import { request } from '@umijs/max';

/** 获取 Token Header */
const getTokenHeader = () => {
  return { 'x-token': localStorage.getItem('token') || '' };
};

// ==================== Token 相关 ====================

/** 验证 Token GET /api/run/valid */
export async function validToken(options?: { [key: string]: any }) {
  return request<void>('/api/run/valid', {
    method: 'GET',
    headers: getTokenHeader(),
    ...(options || {}),
  });
}

/** 获取当前用户信息 */
export async function getCurrentUser(options?: { [key: string]: any }) {
  return request<HTTPRUN.CurrentUser>('/api/run/user', {
    method: 'GET',
    headers: getTokenHeader(),
    ...(options || {}),
  });
}

/** 获取当前用户执行历史 GET /api/run/history */
export async function getExecutionHistory(
  params: {
    page: number;
    pageSize: number;
    tokenName?: string;
    commandName?: string;
    status?: string;
    startTime?: string;
    endTime?: string;
    keyword?: string;
  },
  options?: { [key: string]: any },
) {
  const queryParams = new URLSearchParams();
  queryParams.append('page', String(params.page));
  queryParams.append('pageSize', String(params.pageSize));
  if (params.tokenName) queryParams.append('tokenName', params.tokenName);
  if (params.commandName) queryParams.append('commandName', params.commandName);
  if (params.status) queryParams.append('status', params.status);
  if (params.startTime) queryParams.append('startTime', params.startTime);
  if (params.endTime) queryParams.append('endTime', params.endTime);
  if (params.keyword) queryParams.append('keyword', params.keyword);
  
  return request<HTTPRUN.ExecutionHistoryResponse>(
    `/api/run/history?${queryParams.toString()}`,
    {
      method: 'GET',
      headers: getTokenHeader(),
      ...(options || {}),
    },
  );
}

/** 删除执行记录 DELETE /api/run/history/:id */
export async function deleteExecutionHistoryItem(
  id: number,
  options?: { [key: string]: any },
) {
  return request<{ success: boolean; message: string }>(
    `/api/run/history/${id}`,
    {
      method: 'DELETE',
      headers: getTokenHeader(),
      ...(options || {}),
    },
  );
}

/** 批量删除执行记录 DELETE /api/run/history */
export async function deleteExecutionHistoryBatch(
  ids: number[],
  options?: { [key: string]: any },
) {
  return request<{ success: boolean; message: string; deleted: number }>(
    `/api/run/history?ids=${ids.join(',')}`,
    {
      method: 'DELETE',
      headers: getTokenHeader(),
      ...(options || {}),
    },
  );
}

/** 清空当前用户执行记录 DELETE /api/run/history/clear */
export async function clearExecutionHistory(
  options?: { [key: string]: any },
) {
  return request<{ success: boolean; message: string; deleted: number }>(
    '/api/run/history/clear',
    {
      method: 'DELETE',
      headers: getTokenHeader(),
      ...(options || {}),
    },
  );
}

// ==================== 命令相关 ====================

/** 获取用户命令列表 GET /api/run/commands */
export async function getUserCommandList(options?: { [key: string]: any }) {
  return request<HTTPRUN.CommandItem[]>('/api/run/commands', {
    method: 'GET',
    headers: getTokenHeader(),
    ...(options || {}),
  });
}

/** 获取管理员命令列表 GET /api/admin/commands */
export async function getCommandList(options?: { [key: string]: any }) {
  return request<HTTPRUN.CommandItem[]>('/api/admin/commands', {
    method: 'GET',
    headers: getTokenHeader(),
    ...(options || {}),
  });
}

/** 运行命令 POST /api/run */
export async function runCommand(
  name: string,
  params: HTTPRUN.Param[],
  env: HTTPRUN.Environment[],
  options?: { [key: string]: any },
) {
  return request<HTTPRUN.CommandOutputResponse>('/api/run', {
    method: 'POST',
    headers: {
      ...getTokenHeader(),
      'Content-Type': 'application/json',
    },
    data: { name, params, env },
    ...(options || {}),
  });
}

/** 创建命令 POST /api/admin/command */
export async function createCommand(
  command: HTTPRUN.CommandItem,
  options?: { [key: string]: any },
) {
  return request<HTTPRUN.CreateCommandResponse>('/api/admin/command', {
    method: 'POST',
    headers: {
      ...getTokenHeader(),
      'Content-Type': 'application/json',
    },
    data: command,
    ...(options || {}),
  });
}

/** 更新命令 PUT /api/admin/command/:name */
export async function updateCommand(
  name: string,
  command: HTTPRUN.CommandItem,
  options?: { [key: string]: any },
) {
  return request<HTTPRUN.CreateCommandResponse>(`/api/admin/command/${encodeURIComponent(name)}`, {
    method: 'PUT',
    headers: {
      ...getTokenHeader(),
      'Content-Type': 'application/json',
    },
    data: command,
    ...(options || {}),
  });
}

/** 删除命令 DELETE /api/admin/commands */
export async function deleteCommand(
  names: string[],
  options?: { [key: string]: any },
) {
  return request<void>(`/api/admin/commands?name=${names.join(',')}`, {
    method: 'DELETE',
    headers: getTokenHeader(),
    ...(options || {}),
  });
}

// ==================== Token 管理 ====================

/** 获取 Token 列表 GET /api/admin/tokens */
export async function getTokenList(
  options?: { [key: string]: any },
) {
  return request<HTTPRUN.TokenItem[]>(
    `/api/admin/tokens`,
    {
      method: 'GET',
      headers: getTokenHeader(),
      ...(options || {}),
    },
  );
}

/** 创建 Token POST /api/admin/token */
export async function createToken(
  data: HTTPRUN.CreateTokenRequest,
  options?: { [key: string]: any },
) {
  return request<HTTPRUN.CreateTokenResponse>('/api/admin/token', {
    method: 'POST',
    headers: {
      ...getTokenHeader(),
      'Content-Type': 'application/json',
    },
    data,
    ...(options || {}),
  });
}

/** 删除 Token DELETE /api/admin/token/:id */
export async function deleteToken(
  id: number,
  options?: { [key: string]: any },
) {
  return request<HTTPRUN.RevokeTokenResponse>(`/api/admin/token/${id}`, {
    method: 'DELETE',
    headers: getTokenHeader(),
    ...(options || {}),
  });
}

// ==================== 访问日志 ====================

/** 获取访问日志列表 GET /api/admin/accesslog */
export async function getAccessLogList(
  params: { 
    page: number; 
    pageSize: number; 
    type?: 'command' | 'all';
    keyword?: string;
  },
  options?: { [key: string]: any },
) {
  const queryParams = new URLSearchParams();
  queryParams.append('page', String(params.page));
  queryParams.append('pageSize', String(params.pageSize));
  if (params.type) queryParams.append('type', params.type);
  if (params.keyword) queryParams.append('keyword', params.keyword);
  
  return request<HTTPRUN.AccessLogListResponse>(
    `/api/admin/accesslog?${queryParams.toString()}`,
    {
      method: 'GET',
      headers: getTokenHeader(),
      ...(options || {}),
    },
  );
}

/** 删除访问日志 DELETE /api/admin/accesslog */
export async function deleteAccessLog(
  id: number,
  options?: { [key: string]: any },
) {
  return request<{ id: number }>(`/api/admin/accesslog?id=${id}`, {
    method: 'DELETE',
    headers: getTokenHeader(),
    ...(options || {}),
  });
}
