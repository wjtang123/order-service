package com.demo.order.service;

import com.demo.order.entity.Order;
import com.demo.order.entity.StockDeductedEvent;
import com.demo.order.mapper.ConsumedMessageMapper;
import com.demo.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    @Autowired private OrderMapper           orderMapper;
    @Autowired private ConsumedMessageMapper consumedMapper;

    /**
     * 消费库存扣减事件，创建订单
     *
     * 幂等保证（非常重要！）：
     *   MQ 的 at-least-once 语义意味着消息可能重复投递。
     *   如果不做幂等，同一条消息消费两次 → 创建两个订单。
     *
     *   方案：用 consumed_message 表记录已消费的 messageId（有 UNIQUE 约束）
     *   第一次消费：INSERT 成功 → 创建订单
     *   重复消费：INSERT 抛 DuplicateKeyException → 直接忽略
     *
     *   整个操作（记录消费+创建订单）在同一事务里，
     *   防止"记录了消费但订单没创建"的情况。
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(StockDeductedEvent event) {

        String messageId = event.getMessageId();

        // 幂等检查：尝试插入消费记录
        try {
            consumedMapper.insert(messageId);
        } catch (DuplicateKeyException e) {
            // 消息已经消费过，直接忽略（幂等）
            System.out.println("[OrderService] 消息已消费，跳过: msgId=" + messageId);
            return;
        }

        // 检查订单是否已存在（双重保障）
        if (orderMapper.findBySourceMsgId(messageId) != null) {
            System.out.println("[OrderService] 订单已存在，跳过: msgId=" + messageId);
            return;
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setUserId(event.getUserId());
        order.setProductId(event.getProductId());
        order.setProductName(event.getProductName());
        order.setQuantity(event.getQuantity());
        order.setStatus("CREATED");
        order.setSourceMsgId(messageId);

        orderMapper.insert(order);
        System.out.println("[OrderService] ✓ 订单创建成功: " + order.getOrderNo()
                + " msgId=" + messageId);
    }
}
