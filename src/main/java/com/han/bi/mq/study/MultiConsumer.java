package com.han.bi.mq.study;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadPoolExecutor;

public class MultiConsumer {

    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {
        // 建立连接
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.5.100");
        factory.setUsername("root");
        factory.setPassword("491001");
        factory.setPort(5672);
        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        for (int i = 0; i < 2; i++) {
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            // 每个消费者每次最多处理一个任务
            channel.basicQos(1);

            // 定义如何处理消费
            int finalI = i;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    System.out.println(" [x] Received 消费者:" + finalI + ", msg:" + message + "");
                    // 处理工作，模拟服务器处理能力有限
                    Thread.sleep(10000);
                    // 手动确认消息，消息队列收到后就会从队列中移除
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // 拒绝确认并从队列中移除，false表示此条消息不再重新入队
                    channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
                } finally {
                    System.out.println(" [x] Done");
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            // 开启消费监听, autoAck = false
            channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });
            /*
            一、basicAck方法用于显式地确认消息已被消费处理。当消费者成功处理一条消息后，可以调用basicAck方法通知RabbitMQ，使其删除该消息。该方法接收一个deliveryTag参数，指定要确认的消息的标识符。

            二、basicNack方法用于显式地否定消息。与basicAck方法不同，basicNack方法可以一次性拒绝多条消息。它可以接收三个参数：
            1.deliveryTag：表示要拒绝的消息的标识符。
            2.multiple：表示是否拒绝给定标识符之前的所有未确认消息。如果为true，则拒绝给定标识符之前的所有消息；如果为false，则只拒绝指定标识符的消息。
            3.requeue：表示是否将消息重新排队，如果为true，则消息将会重新进入队列，如果为false，则消息将会被丢弃。

            三、basicReject方法用于单独地否定消息，一次只能拒绝一条消息。它接收两个参数：
            1.deliveryTag：表示要拒绝的消息的标识符。
            2.requeue：表示是否将消息重新排队，如果为true，则消息将会重新进入队列，如果为false，则消息将会被丢弃。
             */
        }
    }
}