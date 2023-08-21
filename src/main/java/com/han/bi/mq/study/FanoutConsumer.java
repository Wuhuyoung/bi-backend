package com.han.bi.mq.study;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class FanoutConsumer {
  private static final String EXCHANGE_NAME = "fanout-exchange";

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("192.168.5.100");
    factory.setUsername("root");
    factory.setPassword("491001");
    factory.setPort(5672);

    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    // 创建交换机
    channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

    // 创建消息队列1，将队列绑定到交换机上
    String queueName1 = "fanout-queue1";
    channel.queueDeclare(queueName1, true, false, false, null);
    channel.queueBind(queueName1, EXCHANGE_NAME, "");
    // 创建消息队列2，将队列绑定到交换机上
    String queueName2 = "fanout-queue2";
    channel.queueDeclare(queueName2, true, false, false, null);
    channel.queueBind(queueName2, EXCHANGE_NAME, "");

    DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        System.out.println(" [1] Received '" + message + "'");
    };

    channel.basicConsume(queueName1, true, deliverCallback1, consumerTag -> { });

    DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      System.out.println(" [2] Received '" + message + "'");
    };
    channel.basicConsume(queueName2, true, deliverCallback2, consumerTag -> { });
  }
}