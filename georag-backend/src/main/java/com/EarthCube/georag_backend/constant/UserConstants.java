package com.EarthCube.georag_backend.constant;

public class UserConstants {

    // ================== 账号状态 (对应 sys_user.status) ==================
    /** 账号状态：0 - 禁用 / 冻结 */
    public static final Integer STATUS_DISABLE = 0;

    /** 账号状态：1 - 正常 */
    public static final Integer STATUS_NORMAL = 1;

    /** 账号状态：2 - 未激活 (注册后的默认状态) */
    public static final Integer STATUS_INACTIVE = 2;

    /** 账号状态：3 - 注销 (软删除) */
    public static final Integer STATUS_DELETED = 3;


    // ================== 性别 (对应 sys_user.gender) ==================
    /** 性别：0 - 保密 */
    public static final Integer GENDER_SECRET = 0;

    /** 性别：1 - 男 */
    public static final Integer GENDER_MALE = 1;

    /** 性别：2 - 女 */
    public static final Integer GENDER_FEMALE = 2;


    // ================== 业务默认值 ==================
    /** 默认头像 (MinIO 地址) */
    public static final String DEFAULT_AVATAR = "http://localhost:9000/avatar/default.png";

    /** 注册赠送的默认积分 (M001DS定义中Quota自动生成) */
    public static final Integer DEFAULT_QUOTA = 10;


    // ================== Redis Key 前缀 ==================
    /** * 邮箱验证码 Key 前缀
     * 完整格式: verify:code:email:zhangsan@xxx.com
     */
    public static final String REDIS_VERIFY_EMAIL_KEY = "verify:code:email:";

    /** * 验证码防刷 Key 前缀 (限制60秒发送一次)
     * 完整格式: verify:limit:email:zhangsan@xxx.com
     */
    public static final String REDIS_VERIFY_LIMIT_KEY = "verify:limit:email:";


    // ================== 时间常量 ==================
    /** 验证码有效期 (5分钟 = 300秒) */
    public static final Long CODE_TTL = 300L;

    /** 验证码发送间隔限制 (60秒) */
    public static final Long CODE_INTERVAL = 60L;
}