import request from '../utils/request';
import type { ApiResponse } from '../types/api';

/**
 * M001DS 抽象数据结构对应的 TypeScript 接口
 */
export interface UserInfo {
  id: string;            // 用户唯一标识
  username: string;      // 登录唯一凭证
  email: string;         // 注册邮箱
  nickname: string;      // 昵称，支持 Emoji
  avatar?: string;       // 头像 URL
  quota: number;         // 剩余积分
  status: number;        // 账号状态
  gender?: number;       // 性别
  region?: string;       // 地区
  settings?: Record<string, any>; // 扩展字段
}

/**
 * 接口请求参数定义
 */

// 3. 用户登陆请求参数
export interface LoginParams {
  account: string;  // 支持用户名 / 邮箱 / 手机号
  password: string; // 登录密码
}

// 2. 用户注册请求参数
export interface RegisterParams {
  username: string;   // 3-20 位字母/数字
  password: string;   // 8-20 位字母+数字
  email: string;      // 需与发送验证码的邮箱一致
  verifyCode: string; // 6 位数字验证码
  nickname: string;   // 1-64 位
  phone?: string;     // 选填
}

// 5. 修改个人信息请求参数
export interface ProfileUpdateParams {
  nickname?: string;
  gender?: number;
  region?: string;
  settings?: Record<string, any>;
}

/**
 * M001API 接口映射
 */
export const userApi = {
  
  /**
   * 1. 发送邮箱验证码 (注册专用)
   * 验证码有效期 5 分钟
   */
  sendRegisterCode(email: string): Promise<ApiResponse<null>> {
    return request.post('/send-register-code', { email });
  },

  /**
   * 2. 用户注册
   * 验证验证码后直接返回登录 Token
   */
  register(data: RegisterParams): Promise<ApiResponse<{ id: string; token: string }>> {
    return request.post('/register', data);
  },

  /**
   * 3. 用户登陆
   * 返回 JWT Token 及基础画像
   */
  login(data: LoginParams): Promise<ApiResponse<{ token: string; nickname: string; avatar: string }>> {
    return request.post('/login', data);
  },

  /**
   * 4. 上传头像
   * 仅支持 jpg/png，大小≤5MB
   */
  uploadAvatar(file: File): Promise<ApiResponse<{ avatarUrl: string }>> {
    const formData = new FormData();
    formData.append('file', file);
    return request.post('/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },

  /**
   * 5. 修改个人信息
   */
  updateProfile(data: ProfileUpdateParams): Promise<ApiResponse<null>> {
    return request.put('/profile', data);
  },

  /**
   * 6. 查看个人信息
   * 通过 Token 解析用户 ID
   */
  getProfile(): Promise<ApiResponse<UserInfo & { genderDesc: string; statusDesc: string; createTime: string }>> {
    return request.get('/profile');
  },

  /**
   * 7. 账号注销
   * 需要二次验证密码
   */
  cancelAccount(password: string): Promise<ApiResponse<null>> {
    return request.post('/cancel', { password });
  },

  /**
   * 8. 查看剩余积分
   */
  getQuota(): Promise<ApiResponse<{ quota: number; quotaDesc: string }>> {
    return request.get('/quota');
  }
};