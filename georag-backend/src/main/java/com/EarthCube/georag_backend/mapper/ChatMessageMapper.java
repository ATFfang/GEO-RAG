package com.EarthCube.georag_backend.mapper;

import com.EarthCube.georag_backend.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息 Mapper 接口
 * 继承 BaseMapper 后，自动拥有 insert, delete, update, selectById, selectList 等方法
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    // 除非你有特别复杂的 SQL 查询（如多表联查），否则这里什么都不用写
}