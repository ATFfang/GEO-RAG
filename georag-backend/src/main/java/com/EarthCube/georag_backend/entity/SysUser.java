package com.EarthCube.georag_backend.entity;

import com.EarthCube.georag_backend.enums.GenderEnum;
import com.EarthCube.georag_backend.enums.UserStatusEnum;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.JdbcType;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 系统用户实体类
 * 对应表名：sys_user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "sys_user", autoResultMap = true) // autoResultMap 必须为 true 才能让 JSON 处理器生效
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识 (UUID)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 登录用户名
     */
    private String username;

    /**
     * 密码 (BCrypt加密)
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 性别 (0-保密, 1-男, 2-女)
     */
    private GenderEnum gender;

    /**
     * 地区
     */
    private String region;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 剩余积分
     */
    private Integer quota;

    /**
     * 扩展字段 (JSON)
     * 存储前端偏好，如深色模式等
     */
    private String settings;

    /**
     * 账号状态
     */
    private UserStatusEnum status;

    /**
     * 创建时间
     * 插入时自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 插入和更新时自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;
}
