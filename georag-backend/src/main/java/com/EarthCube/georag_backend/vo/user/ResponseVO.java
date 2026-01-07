package com.EarthCube.georag_backend.vo.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseVO {
    /** 用户ID */
    private String id;

    /** 登录凭证 */
    private String token;
}