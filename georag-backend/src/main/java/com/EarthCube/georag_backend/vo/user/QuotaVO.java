package com.EarthCube.georag_backend.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuotaVO {
    private Integer quota;
    private String quotaDesc;
}