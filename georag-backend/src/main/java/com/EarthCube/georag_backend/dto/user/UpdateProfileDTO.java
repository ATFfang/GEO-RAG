package com.EarthCube.georag_backend.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class UpdateProfileDTO {

    @Size(max = 64, message = "昵称长度不能超过64位")
    private String nickname;

    @Min(value = 0, message = "性别类型错误")
    @Max(value = 2, message = "性别类型错误")
    private Integer gender;

    @Size(max = 100, message = "地区长度不能超过100位")
    private String region;

    // 前端传来的 JSON 对象，后端用 Map 接收
    private Map<String, Object> settings;
}
