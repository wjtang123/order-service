package com.demo.order.mapper;

import com.demo.order.entity.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO orders(order_no, user_id, product_id, product_name, quantity, status, source_msg_id) " +
            "VALUES(#{orderNo}, #{userId}, #{productId}, #{productName}, #{quantity}, #{status}, #{sourceMsgId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Select("SELECT * FROM orders WHERE source_msg_id = #{sourceMsgId}")
    Order findBySourceMsgId(@Param("sourceMsgId") String sourceMsgId);

    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Order> findByUserId(@Param("userId") String userId);

    @Select("SELECT * FROM orders ORDER BY created_at DESC LIMIT 20")
    List<Order> findRecent();
}
