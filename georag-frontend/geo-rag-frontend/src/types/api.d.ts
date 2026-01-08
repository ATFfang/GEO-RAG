export interface ApiResponse<T = any> {
  code: number;      // 状态码：200-成功, 4001-用户名已存在等
  msg: string;       // 提示信息
  data: T;           // 业务数据
  timestamp: number; // 时间戳
}