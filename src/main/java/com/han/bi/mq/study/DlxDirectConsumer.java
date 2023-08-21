package com.han.bi.mq.study;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxDirectConsumer {

    private static final String EXCHANGE_NAME = "direct2-exchange";
    private static final String DEAD_EXCHANGE_NAME = "dlx-direct-exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.5.100");
        factory.setUsername("root");
        factory.setPassword("491001");
        factory.setPort(5672);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "direct");
        // 指定私信队列参数
        Map<String, Object> args = new HashMap<>();
        // 要绑定到哪个死信交换机
        args.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        // 指定死信要转发到哪个死信队列
        args.put("x-dead-letter-routing-key", "dlx1");

        // 创建消息队列1，将队列绑定到交换机上
        String queueName1 = "xiaoli-queue";
        channel.queueDeclare(queueName1, true, false, false, args);
        channel.queueBind(queueName1, EXCHANGE_NAME, "xiaoli");

        // 指定私信队列参数
        Map<String, Object> args2 = new HashMap<>();
        // 要绑定到哪个死信交换机
        args2.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        // 指定死信要转发到哪个死信队列
        args2.put("x-dead-letter-routing-key", "dlx2");

        // 创建消息队列2，将队列绑定到交换机上
        String queueName2 = "xiaoliu-queue";
        channel.queueDeclare(queueName2, true, false, false, args2);
        channel.queueBind(queueName2, EXCHANGE_NAME, "xiaoliu");

        DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [xiaoyu] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
            // 拒绝消息，让消息送到死信队列中
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        };

        DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [xiaohan] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
            // 拒绝消息，让消息送到死信队列中
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        };

        channel.basicConsume(queueName1, false, deliverCallback1, consumerTag -> {
        });
        channel.basicConsume(queueName2, false, deliverCallback2, consumerTag -> {
        });
    }
}