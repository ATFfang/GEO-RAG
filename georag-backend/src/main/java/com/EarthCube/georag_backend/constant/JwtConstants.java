package com.EarthCube.georag_backend.constant;

public class JwtConstants {

    /** 请求头 Key */
    public static final String TOKEN_HEADER = "Authorization";

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Redis 中存储 Token 的 Key 前缀 (如果做单点登录或踢人功能会用到) */
    public static final String LOGIN_USER_KEY = "login_user:";
}
