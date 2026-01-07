package com.EarthCube.georag_backend.constant;

public class RegexConstants {

    /** 手机号正则 (简单版: 1开头+10位数字) */
    public static final String PHONE_REGEX = "^1\\d{10}$";

    /** 邮箱正则 */
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    /** 用户名正则 (字母数字，4到32位) */
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9]{4,32}$";

    /** 密码正则 (至少8位，包含字母和数字) */
    // public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$";
    // 你的文档说必须包含字母+数字，可含特殊字符，简单校验长度即可，复杂的在代码逻辑判断
}