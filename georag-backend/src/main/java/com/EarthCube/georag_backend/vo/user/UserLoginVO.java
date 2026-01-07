package com.EarthCube.georag_backend.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginVO {
    // JWT 令牌
    private String token;

    // 用户昵称
    private String nickname;

    // 头像 URL
    private String avatar;
}
