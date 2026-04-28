package com.demo.order.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * RabbitMQ 消费端配置
 *
 * 核心：配置手动ACK + 消费失败重试策略
 */
@Configuration
public class RabbitConsumerConfig {

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 自定义监听容器工厂
     * 控制：并发数、ACK模式、重试策略
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());

        // 手动 ACK（MANUAL = 代码里手动调 basicAck/basicNack）
        factory.setAcknowledgeMode(
                org.springframework.amqp.core.AcknowledgeMode.MANUAL);

        // 并发消费者数量（可根据压力调整）
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);

        // 预取数量：每次从队列拉取多少条消息
        // 设小一点（1~5），避免一个消费者拉太多消息但处理慢，其他消费者空闲
        factory.setPrefetchCount(2);

        return factory;
    }

    /**
     * 消息最终恢复器：超过重试次数后的处理
     * RejectAndDontRequeueRecoverer = 拒绝消息且不重新入队（进死信队列）
     */
    @Bean
    public MessageRecoverer messageRecoverer() {
        return new RejectAndDontRequeueRecoverer();
    }
}
