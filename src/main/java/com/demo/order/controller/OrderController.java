package com.demo.order.controller;

import com.demo.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired private OrderMapper orderMapper;

    /** 查看最近20条订单（验证消费结果） */
    @GetMapping
    public List<?> listOrders() {
        return orderMapper.findRecent();
    }

    /** 按用户查订单 */
    @GetMapping("/user/{userId}")
    public List<?> getByUser(@PathVariable String userId) {
        return orderMapper.findByUserId(userId);
    }
}
