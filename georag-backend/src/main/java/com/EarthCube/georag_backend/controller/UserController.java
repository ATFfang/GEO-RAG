package com.EarthCube.georag_backend.controller;

import com.EarthCube.georag_backend.common.result.Result;
import com.EarthCube.georag_backend.dto.user.*;
import com.EarthCube.georag_backend.service.SysUserService;
import com.EarthCube.georag_backend.vo.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 1. 发送注册验证码
     */
    @PostMapping("/send-register-code")
    public Result<Void> sendRegisterCode(@RequestBody SendRegisterCodeDTO dto) {
        sysUserService.sendRegisterCode(dto);
        return Result.success("验证码已发送，有效期5分钟", null);
    }

    /**
     * 2. 用户注册
     */
    @PostMapping("/register")
    public Result<ResponseVO> register(@RequestBody UserRegisterDTO dto) {
        ResponseVO vo = sysUserService.register(dto);
        return Result.success("注册成功", vo);
    }

    /**
     * 3. 用户登录
     */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO dto) {
        UserLoginVO vo = sysUserService.login(dto);
        return Result.success("登录成功", vo);
    }

    /**
     * 4. 上传头像
     * 注意：Content-Type 是 multipart/form-data，不需要 @RequestBody
     */
    @PostMapping("/avatar")
    public Result<AvatarUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        AvatarUploadVO vo = sysUserService.uploadAvatar(file);
        return Result.success("头像上传成功", vo);
    }

    /**
     * 5. 修改个人信息
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileDTO dto) {
        sysUserService.updateProfile(dto);
        return Result.success("个人信息修改成功", null);
    }

    /**
     * 6. 查看个人信息
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        UserProfileVO vo = sysUserService.getProfile();
        return Result.success("查询成功", vo);
    }

    /**
     * 7. 账号注销
     */
    @PostMapping("/cancel")
    public Result<Void> cancelAccount(@RequestBody CancelAccountDTO dto) {
        sysUserService.cancelAccount(dto);
        return Result.success("账号注销成功", null);
    }

    /**
     * 8. 查看剩余积分
     */
    @GetMapping("/quota")
    public Result<QuotaVO> getQuota() {
        QuotaVO vo = sysUserService.getQuota();
        return Result.success("查询成功", vo);
    }
}