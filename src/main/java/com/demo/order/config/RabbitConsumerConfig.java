package com.demo.order.config;

import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * RabbitMQ 消费端配置
 *
 * 核心：配置手动ACK + 消费失败重试策略
 *
 * 注意：此处显式声明了 rabbitListenerContainerFactory Bean，
 *       application.yml 中 listener.simple.* 的配置对该工厂全部失效，
 *       重试策略、ACK模式均以此处为准。
 */
@Configuration
public class RabbitConsumerConfig {

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 自定义监听容器工厂
     * 控制：并发数、ACK模式、重试策略、消息恢复器
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());

        // 手动 ACK：消费成功后代码里主动调 basicAck，失败由 RetryTemplate + Recoverer 处理
        factory.setAcknowledgeMode(
                org.springframework.amqp.core.AcknowledgeMode.MANUAL);

        // 并发消费者数量（可根据压力调整）
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);

        // 预取数量：每次从队列拉取多少条消息
        // 设小一点（1~5），避免一个消费者拉太多消息但处理慢，其他消费者空闲
        factory.setPrefetchCount(2);

        // ---- 重试策略 + 消息恢复器，通过 RetryOperationsInterceptor 组合 ----
        // MessageRecoverer 没有独立的 setter，必须打包进 Interceptor，再用 setAdviceChain 注入
        RetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateless()
                .maxAttempts(3)                              // 最多尝试3次（含第1次）
                .backOffOptions(2000, 2.0, 10000)            // 初始2s，指数×2，最大10s
                .recoverer(new RejectAndDontRequeueRecoverer()) // 耗尽后 nack → 进死信队列
                .build();

        factory.setAdviceChain(interceptor);

        return factory;
    }
}
