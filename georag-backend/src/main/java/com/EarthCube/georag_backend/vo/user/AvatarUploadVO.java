package com.EarthCube.georag_backend.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AvatarUploadVO {
    // 上传后的 MinIO 访问地址
    private String avatarUrl;
}