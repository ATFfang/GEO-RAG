import axios, { type AxiosResponse } from 'axios';
import type { ApiResponse } from '../types/api';

const request = axios.create({
  baseURL: '/api/v1/users', //
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

export default request;