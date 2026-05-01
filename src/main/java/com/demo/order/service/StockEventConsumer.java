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
 * 消费流程：
 *   1. 收到消息，反序列化 JSON → StockDeductedEvent
 *   2. 调用 OrderService.createOrder()（内部做幂等检查）
 *   3. 成功 → channel.basicAck，消息从队列删除
 *   4. 失败 → 只抛出异常，由 RabbitConsumerConfig 中的 RetryTemplate 负责重试
 *             重试耗尽 → RejectAndDontRequeueRecoverer 执行 basicNack(requeue=false)
 *                      → 触发 stock-service 中配置的死信路由 → 进入 dead.queue
 *
 * 注意：catch 块中不能手动调 basicNack，否则消息在第一次失败时就立刻进死信，
 *       RetryTemplate 的重试将完全失效。
 */
@Component
public class StockEventConsumer {

    @Autowired private OrderService orderService;

    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * containerFactory 必须显式指定，确保使用配置了重试策略的自定义工厂
     */
    @RabbitListener(queues = "stock.deducted.queue", containerFactory = "rabbitListenerContainerFactory")
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
            System.err.println("[Consumer] ✗ 消费失败，等待重试: " + e.getMessage());

            // ❌ 不在此处调用 basicNack
            //    原因：basicNack 会立即通知 RabbitMQ 拒绝消息，
            //          导致消息在第一次失败时就进入死信队列，
            //          RetryTemplate 配置的重试次数完全无效。
            //
            // ✅ 只抛出异常，交给 RetryTemplate 决定是否继续重试：
            //    - 未达到 max-attempts：等待退避时间后重试本方法
            //    - 达到 max-attempts：调用 RejectAndDontRequeueRecoverer
            //                        → basicNack(requeue=false) → 进死信队列
            throw e;
        }
    }
}
