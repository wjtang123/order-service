package com.demo.order.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface ConsumedMessageMapper {

    /**
     * 尝试插入消费记录（UNIQUE约束保证幂等）
     * 插入成功返回1，消息已消费过则抛重复键异常
     */
    @Insert("INSERT INTO consumed_message(message_id) VALUES(#{messageId})")
    int insert(@Param("messageId") String messageId);

    @Select("SELECT COUNT(*) FROM consumed_message WHERE message_id = #{messageId}")
    int exists(@Param("messageId") String messageId);
}
