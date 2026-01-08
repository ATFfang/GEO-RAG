package com.EarthCube.georag_backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.EarthCube.georag_backend.common.context.UserContext;
import com.EarthCube.georag_backend.common.exception.BusinessException;
import com.EarthCube.georag_backend.common.exception.ValidateException;
import com.EarthCube.georag_backend.constant.UserConstants;
import com.EarthCube.georag_backend.dto.user.*;
import com.EarthCube.georag_backend.entity.SysUser;
import com.EarthCube.georag_backend.enums.GenderEnum;
import com.EarthCube.georag_backend.enums.UserStatusEnum;
import com.EarthCube.georag_backend.mapper.SysUserMapper;
import com.EarthCube.georag_backend.service.SysUserService;
import com.EarthCube.georag_backend.util.*;
import com.EarthCube.georag_backend.vo.user.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private PasswordUtil passwordUtil;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private MinioUtil minioUtil;
    @Autowired
    private EmailUtil emailUtil;

    @Override
    public void sendRegisterCode(SendRegisterCodeDTO dto) {
        String email = dto.getEmail();

        // 1. 校验邮箱是否已注册
        Long count = this.lambdaQuery().eq(SysUser::getEmail, email).count();
        if (count > 0) {
            throw new BusinessException(4003, "该邮箱已被注册");
        }

        // 2. 校验发送频率 (60秒内不能重复发)
        String limitKey = UserConstants.REDIS_VERIFY_LIMIT_KEY + email;
        if (redisUtil.hasKey(limitKey)) {
            throw new BusinessException(4004, "验证码发送过于频繁，请稍后重试");
        }

        // 3. 生成 6 位随机验证码
        String code = RandomUtil.randomNumbers(6);

        // 4. 存入 Redis (有效期 5 分钟)
        String codeKey = UserConstants.REDIS_VERIFY_EMAIL_KEY + email;
        redisUtil.set(codeKey, code, UserConstants.CODE_TTL);
        // 设置 60秒 防刷限制
        redisUtil.set(limitKey, "1", UserConstants.CODE_INTERVAL);

        // 5. 发送邮件 (这里先打印到控制台模拟)
        log.info("【模拟邮件发送】向 {} 发送验证码: {}", email, code);
        emailUtil.send(email, "注册验证码", "您的验证码是：" + code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO register(UserRegisterDTO dto) {
        // 1. 校验验证码
        String codeKey = UserConstants.REDIS_VERIFY_EMAIL_KEY + dto.getEmail();
        Object cacheCode = redisUtil.get(codeKey);
        if (cacheCode == null || !cacheCode.toString().equals(dto.getVerifyCode())) {
            throw new BusinessException(4005, "验证码错误或已过期");
        }

        // 2. 校验用户名唯一
        Long nameCount = this.lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).count();
        if (nameCount > 0) {
            throw new BusinessException(4001, "用户名已存在");
        }

        // 3. 构建用户对象
        SysUser user = new SysUser();
        BeanUtil.copyProperties(dto, user);

        user.setId(UUID.randomUUID().toString()); // 生成 UUID
        user.setPassword(PasswordUtil.encode(dto.getPassword())); // 密码加密
        user.setStatus(UserStatusEnum.NORMAL);
        user.setQuota(UserConstants.DEFAULT_QUOTA);
        user.setAvatar(UserConstants.DEFAULT_AVATAR);
        user.setGender(GenderEnum.SECRET);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 4. 落库
        this.save(user);

        // 5. 注册成功后直接登录，生成 Token
        String token = jwtUtil.createToken(user.getId(), user.getUsername());

        // 6. 清除验证码
        // redisUtil.del(codeKey); // 实际开发中可以删，也可以留着过期

        return ResponseVO.builder()
                .id(user.getId())
                .token(token)
                .build();
    }

    @Override
    public UserLoginVO login(UserLoginDTO dto) {
        String account = dto.getAccount();

        // 1. 根据 账号/邮箱/手机号 查询用户
        SysUser user = this.lambdaQuery()
                .eq(SysUser::getUsername, account)
                .or().eq(SysUser::getEmail, account)
                .or().eq(SysUser::getPhone, account)
                .one();

        // 2. 用户不存在或密码错误
        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new ValidateException("用户名或密码错误");
        }

        // 3. 检查账号状态
        if (UserStatusEnum.BANNED.equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        // 4. 更新登录信息 (最后登录时间/IP)
        user.setLastLoginTime(LocalDateTime.now());
        // user.setLastLoginIp(...) // 如果需要记录IP，可以从Request里取，这里先略过
        this.updateById(user);

        // 5. 生成 Token
        String token = jwtUtil.createToken(user.getId(), user.getUsername());

        // 6. 组装返回 VO
        return UserLoginVO.builder()
                .token(token)
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public AvatarUploadVO uploadAvatar(MultipartFile file) {
        // 1. 获取当前用户ID
        String userId = UserContext.getUserId();

        try {
            // 2. 上传头像
            String avatarUrl = minioUtil.uploadAvatar(file);

            // 3. 更新数据库
            SysUser updateUser = new SysUser();
            updateUser.setId(userId);
            updateUser.setAvatar(avatarUrl);
            this.updateById(updateUser);

            return new AvatarUploadVO(avatarUrl);

        } catch (Exception e) {
            log.error("头像上传失败", e);
            throw new BusinessException("头像上传失败: " + e.getMessage());
        }
    }

    @Override
    public UserProfileVO getProfile() {
        // 1. 获取当前用户
        SysUser user = this.getById(UserContext.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 2. 转换 VO
        UserProfileVO vo = new UserProfileVO();
        BeanUtil.copyProperties(user, vo);

        // 3. 处理枚举描述等
        vo.setGenderDesc(user.getGender() != null ? user.getGender().getDesc() : "未知");
        vo.setStatusDesc(user.getStatus() != null ? user.getStatus().getDesc() : "未知");

        return vo;
    }

    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        String userId = UserContext.getUserId();

        SysUser user = new SysUser();
        user.setId(userId);
        BeanUtil.copyProperties(dto, user);

        // 只有非空字段会被更新 (MyBatis Plus 默认策略)
        this.updateById(user);
    }

    @Override
    public QuotaVO getQuota() {
        SysUser user = this.getById(UserContext.getUserId());
        return new QuotaVO(user.getQuota(), "剩余" + user.getQuota() + "次API调用");
    }

    @Override
    public void cancelAccount(CancelAccountDTO dto) {
        String userId = UserContext.getUserId();
        SysUser user = this.getById(userId);

        // 1. 二次校验密码
        if (PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误，注销失败");
        }

        // 2. 逻辑删除 (状态改为注销)
        user.setStatus(UserStatusEnum.DELETED);
        this.updateById(user);
    }
}