package com.EarthCube.georag_backend.service.impl;

import com.EarthCube.georag_backend.entity.SysUser;
import com.EarthCube.georag_backend.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security 专用适配器
 * 只负责一件事：根据用户名，去数据库查人，然后封装成 Spring Security 要的格式
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    // 直接注入 Mapper，或者注入 SysUserService 都可以
    private final SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 去数据库查用户 (复用你现有的逻辑)
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );

        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 2. 把你的 SysUser "翻译" 成 Spring Security 的 UserDetails
        return User.builder()
                .username(sysUser.getUsername())
                .password(sysUser.getPassword()) // 注意：这里必须是加密后的密码 (BCrypt)

                // 这里处理账号状态，如果你的 StatusEnum 不一样，需要转换一下
                .disabled(false) // 是否被禁用？可以根据 sysUser.getStatus() 判断
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)

                // 权限列表 (暂时给空，以后可以加 ROLE_USER, ROLE_ADMIN)
                .authorities(Collections.emptyList())
                .build();
    }
}