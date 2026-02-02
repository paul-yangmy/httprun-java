/**
 * 命令流 WebSocket 客户端
 * 用于实时获取命令执行输出
 */

/** 流式消息类型 */
export type StreamMessageType = 
  | 'start' 
  | 'stdout' 
  | 'stderr' 
  | 'error' 
  | 'complete' 
  | 'cancelled';

/** 流式响应消息 */
export interface StreamMessage {
  type: StreamMessageType;
  stdout?: string;
  stderr?: string;
  error?: string;
  exitCode?: number;
  duration?: number;
}

/** 流式请求 */
export interface StreamRequest {
  type: 'run' | 'cancel';
  name?: string;
  params?: Array<{ name: string; value: string }>;
  env?: Array<{ name: string; value: string }>;
  timeout?: number;
}

/** WebSocket 事件回调 */
export interface StreamCallbacks {
  onStart?: () => void;
  onStdout?: (line: string) => void;
  onStderr?: (line: string) => void;
  onError?: (error: string) => void;
  onComplete?: (exitCode: number, duration: number) => void;
  onCancelled?: () => void;
  onClose?: () => void;
}

/**
 * 命令流 WebSocket 客户端
 */
export class CommandStreamClient {
  private ws: WebSocket | null = null;
  private callbacks: StreamCallbacks = {};
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 3;
  private reconnectDelay = 1000;

  /**
   * 连接 WebSocket
   * @param token JWT Token
   * @param callbacks 事件回调
   */
  connect(token: string, callbacks: StreamCallbacks): Promise<void> {
    return new Promise((resolve, reject) => {
      this.callbacks = callbacks;

      // 构建 WebSocket URL
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = window.location.host;
      const url = `${protocol}//${host}/ws/command/stream?token=${encodeURIComponent(token)}`;

      try {
        this.ws = new WebSocket(url);

        this.ws.onopen = () => {
          console.log('WebSocket connected');
          this.reconnectAttempts = 0;
          resolve();
        };

        this.ws.onmessage = (event) => {
          this.handleMessage(event.data);
        };

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error);
          reject(new Error('WebSocket connection failed'));
        };

        this.ws.onclose = (event) => {
          console.log('WebSocket closed:', event.code, event.reason);
          this.callbacks.onClose?.();
        };
      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * 处理收到的消息
   */
  private handleMessage(data: string) {
    try {
      const msg: StreamMessage = JSON.parse(data);

      switch (msg.type) {
        case 'start':
          this.callbacks.onStart?.();
          break;
        case 'stdout':
          if (msg.stdout) {
            this.callbacks.onStdout?.(msg.stdout);
          }
          break;
        case 'stderr':
          if (msg.stderr) {
            this.callbacks.onStderr?.(msg.stderr);
          }
          break;
        case 'error':
          if (msg.error) {
            this.callbacks.onError?.(msg.error);
          }
          break;
        case 'complete':
          this.callbacks.onComplete?.(msg.exitCode ?? -1, msg.duration ?? 0);
          break;
        case 'cancelled':
          this.callbacks.onCancelled?.();
          break;
      }
    } catch (e) {
      console.error('Parse message error:', e);
    }
  }

  /**
   * 发送执行命令请求
   */
  runCommand(
    name: string, 
    params: Array<{ name: string; value: string }> = [],
    env: Array<{ name: string; value: string }> = [],
    timeout?: number
  ) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket not connected');
    }

    const request: StreamRequest = {
      type: 'run',
      name,
      params,
      env,
      timeout,
    };

    this.ws.send(JSON.stringify(request));
  }

  /**
   * 取消正在执行的命令
   */
  cancel() {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return;
    }

    const request: StreamRequest = { type: 'cancel' };
    this.ws.send(JSON.stringify(request));
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  /**
   * 检查是否已连接
   */
  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
  }
}

// 导出单例实例
export const commandStream = new CommandStreamClient();

export default CommandStreamClient;
