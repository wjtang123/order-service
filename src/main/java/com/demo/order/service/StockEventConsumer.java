package com.demo.order.service;

import com.demo.order.entity.StockDeductedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 库存扣减事件消费者
 *
 * 关键配置（在 application.yml 里）：
 *   acknowledge-mode: manual  → 手动 ACK
 *   retry.enabled: true       → 消费失败自动重试
 *   default-requeue-rejected: false → 超过重试次数转死信队列
 *
 * 消费流程：
 *   1. 收到消息
 *   2. 反序列化 JSON → StockDeductedEvent
 *   3. 调用 OrderService.createOrder()（内部做幂等检查）
 *   4. 成功 → channel.basicAck（告诉MQ消息处理完了，从队列删除）
 *   5. 失败 → 抛异常 → Spring AMQP 自动重试
 *      重试超次数 → channel.basicNack(false) → 转死信队列
 */
@Component
public class StockEventConsumer {

    @Autowired private OrderService orderService;

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * @RabbitListener 监听指定队列
     * queues：队列名（和 stock-service 的 RabbitConfig 里一致）
     * containerFactory：使用手动ACK的容器工厂（在配置类里定义）
     */
    @RabbitListener(queues = "stock.deducted.queue")
    public void handleStockDeducted(Message message, Channel channel) throws Exception {

        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), "UTF-8");

        System.out.println("[Consumer] 收到消息: " + body);

        try {
            // 1. 反序列化消息体
            StockDeductedEvent event = json.readValue(body, StockDeductedEvent.class);

            // 2. 业务处理（幂等创建订单）
            orderService.createOrder(event);

            // 3. 手动 ACK：告诉 RabbitMQ 消息处理成功，从队列删除
            //    false = 只确认当前这条消息（不批量确认）
            channel.basicAck(deliveryTag, false);
            System.out.println("[Consumer] ✓ ACK 消息 deliveryTag=" + deliveryTag);

        } catch (Exception e) {
            System.err.println("[Consumer] ✗ 消费失败: " + e.getMessage());

            /*
             * basicNack 参数说明：
             *   deliveryTag：当前消息的标识
             *   multiple=false：只拒绝这一条
             *   requeue=false：不重新入队（交给Spring AMQP的重试机制）
             *
             * 因为 application.yml 配置了 retry，Spring AMQP 会自动重试。
             * 超过 max-attempts 且 requeue-rejected=false → 进死信队列。
             */
            channel.basicNack(deliveryTag, false, false);
            throw e;  // 抛出让 Spring AMQP 的重试机制感知到
        }
    }
}
