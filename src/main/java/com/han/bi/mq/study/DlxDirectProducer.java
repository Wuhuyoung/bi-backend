package com.han.bi.mq.study;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class DlxDirectProducer {
    private static final String EXCHANGE_NAME = "direct2-exchange";
    private static final String DEAD_EXCHANGE_NAME = "dlx-direct-exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("192.168.5.100");
        factory.setUsername("root");
        factory.setPassword("491001");
        factory.setPort(5672);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            // 声明死信交换机
            channel.exchangeDeclare(DEAD_EXCHANGE_NAME, "direct");

            // 创建死信队列1，将队列绑定到死信交换机上
            String queueName1 = "dlx1-queue";
            channel.queueDeclare(queueName1, true, false, false, null);
            channel.queueBind(queueName1, DEAD_EXCHANGE_NAME, "dlx1");

            // 创建死信队列2，将队列绑定到死信交换机上
            String queueName2 = "dlx2-queue";
            channel.queueDeclare(queueName2, true, false, false, null);
            channel.queueBind(queueName2, DEAD_EXCHANGE_NAME, "dlx2");

            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String userInput = scanner.nextLine();
                String[] strings = userInput.split(" ");
                if (strings.length < 1) {
                    continue;
                }
                String message = strings[0];
                String routingKey = strings[1];

                channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes(StandardCharsets.UTF_8));
                System.out.println(" [x] Sent '" + message + "' with routeKey:'" + routingKey + "'");
            }
        }
    }
    //..
}