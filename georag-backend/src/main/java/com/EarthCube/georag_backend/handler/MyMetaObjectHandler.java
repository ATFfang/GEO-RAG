package com.EarthCube.georag_backend.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 自动填充处理器
 * 用于自动维护 create_time 和 update_time
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 对应 Entity 中的 createTime 字段
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // 对应 Entity 中的 updateTime 字段 (插入时也需要初始值)
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 如果有配额默认值，也可以在这里处理，但通常建议数据库 default 0 或业务层处理
        // this.strictInsertFill(metaObject, "quota", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 对应 Entity 中的 updateTime 字段
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
