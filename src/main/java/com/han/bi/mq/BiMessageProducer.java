package com.han.bi.mq;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BiMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * @param message
     */
    public void sendMessage(String message) {
        MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setExpiration("120000"); // 消息的过期时间设置为2min
                return message;
            }
        };

        rabbitTemplate.convertAndSend(RabbitmqConfig.BI_EXCHANGE_NAME, RabbitmqConfig.BI_ROUTING_KEY_NAME,
                message,
                messagePostProcessor);
    }
}
