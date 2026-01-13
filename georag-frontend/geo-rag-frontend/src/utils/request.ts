import axios, { type AxiosResponse, type InternalAxiosRequestConfig} from 'axios';
import type { ApiResponse } from '../types/api';

const request = axios.create({
  baseURL: '/api/v1', //
  timeout: 10000,
});

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 这里直接返回 data 部分，也就是包含 code, msg, data 的那个对象
    return response.data as any; 
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 1. 从 LocalStorage 获取 Token
    // 注意：确保这里取值的 key ('token') 和你登录成功后存储的 key 一致
    const token = localStorage.getItem('token');
    
    // 2. 如果有 Token，则添加到 Headers 中
    if (token) {
      // Spring Security 默认标准格式：Bearer + 空格 + Token
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default request;