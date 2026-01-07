package com.EarthCube.georag_backend.service;

import com.EarthCube.georag_backend.dto.user.*;
import com.EarthCube.georag_backend.entity.SysUser;
import com.EarthCube.georag_backend.vo.user.*;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface SysUserService extends IService<SysUser> {

    /**
     * 发送注册验证码
     */
    void sendRegisterCode(SendRegisterCodeDTO dto);

    /**
     * 用户注册
     */
    ResponseVO register(UserRegisterDTO dto);

    /**
     * 用户登录
     */
    UserLoginVO login(UserLoginDTO dto);

    /**
     * 上传头像
     */
    AvatarUploadVO uploadAvatar(MultipartFile file);

    /**
     * 获取个人信息
     */
    UserProfileVO getProfile();

    /**
     * 更新个人信息
     */
    void updateProfile(UpdateProfileDTO dto);

    /**
     * 获取用户积分
     */
    QuotaVO getQuota();

    /**
     * 注销账号
     */
    void cancelAccount(CancelAccountDTO dto);
}