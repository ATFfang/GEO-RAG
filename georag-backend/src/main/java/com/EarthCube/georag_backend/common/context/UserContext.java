package com.EarthCube.georag_backend.common.context;

public class UserContext {
    // 每一个线程都有自己独立的存储空间，互不干扰
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    /**
     * 设置当前登录用户的 ID
     */
    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前登录用户的 ID
     */
    public static String getUserId() {
        return USER_ID.get();
    }

    /**
     * 清除上下文（非常重要！防止内存泄漏和线程复用导致的数据混淆）
     */
    public static void removeUserId() {
        USER_ID.remove();
    }
}
