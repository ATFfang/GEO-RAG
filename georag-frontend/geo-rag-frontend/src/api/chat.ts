import request from '../utils/request';
import type { ApiResponse } from '../types/api';

// --- 类型定义 ---
export interface PageResult<T> {
  current: number;
  pages: number;
  records: T[];
  size: number;
  total: number;
}

export interface ChatSession {
  id: string;
  title: string;
  createTime: string;
  updateTime: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant'; // 对应文档中的 user / assistant
  category: 'text' | 'image'; // 对应文档 category
  context: string; // 对应文档 content 或 context
  createTime: string;
}

// SSE 响应数据结构
export interface StreamMessage {
  sessionId: string;
  messageId: string;
  content: string;
  finishReason: string | null;
}

// --- 接口方法 ---

export const chatApi = {
  /**
   * 获取会话列表
   */
  getSessions(params?: { current?: number; size?: number }): Promise<ApiResponse<PageResult<ChatSession>>> {
    return request.get('/chat/sessions', { params });
  },

  /**
   * 创建新会话
   */
  createSession(title: string): Promise<ApiResponse<string>> {
    return request.post('/chat/sessions', { title });
  },

  /**
   * 修改会话标题
   */
  updateSessionTitle(sessionId: string, title: string): Promise<ApiResponse<null>> {
    return request.patch(`/chat/sessions/${sessionId}`, { title });
  },

  /**
   * 删除会话
   */
  deleteSession(sessionId: string): Promise<ApiResponse<null>> {
    return request.delete(`/chat/sessions/${sessionId}`);
  },

  /**
   * 获取指定会话的历史消息
   */
  getSessionMessages(sessionId: string): Promise<ApiResponse<ChatMessage[]>> {
    return request.get(`/chat/sessions/${sessionId}/messages`);
  }
};