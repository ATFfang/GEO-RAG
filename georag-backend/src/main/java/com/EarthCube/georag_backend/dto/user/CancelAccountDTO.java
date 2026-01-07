package com.EarthCube.georag_backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelAccountDTO {
    @NotBlank(message = "为了您的账号安全，注销前请输入密码")
    private String password;
}
