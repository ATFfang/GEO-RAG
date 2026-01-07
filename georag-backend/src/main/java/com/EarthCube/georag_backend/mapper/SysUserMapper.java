package com.EarthCube.georag_backend.mapper;

import com.EarthCube.georag_backend.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // MyBatis Plus 已经内置了绝大部分 CRUD 方法
    // 如果需要手写 SQL，可以在这里添加方法并在 XML 中实现
}
