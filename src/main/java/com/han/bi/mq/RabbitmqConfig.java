package com.han.bi.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列和交换机 配置类
 */
@Configuration
public class RabbitmqConfig {
    /**
     * 业务交换机
     */
    public static final String BI_EXCHANGE_NAME = "bi_exchange";
    /**
     * 业务队列
     */
    public static final String BI_QUEUE_NAME = "bi_queue";

    /**
     * 业务路由键
     */
    public static final String BI_ROUTING_KEY_NAME = "bi.routing.key";

    /**
     * 死信交换机
     */
    public static final String DEAD_LETTER_EXCHANGE_NAME = "dead.letter.exchange";
    /**
     * 死信队列
     */
    public static final String DEAD_LETTER_QUEUE_NAME = "dead.letter.queue";

    /**
     * 死信队列路由键
     */
    public static final String DEAD_LETTER_QUEUE_ROUTING_KEY_NAME = "dead.letter.queue.routing.key";


    /**
     * 申明业务交换机
     * @return
     */
    @Bean
    public DirectExchange businessExchange(){
        return new DirectExchange(BI_EXCHANGE_NAME);
    }

    /**
     * 申明死信交换机
     * @return
     */
    @Bean
    public DirectExchange deadLetterExchange(){
        return new DirectExchange(DEAD_LETTER_EXCHANGE_NAME);
    }

    /**
     * 申明业务队列
     * @return
     */
    @Bean
    public Queue queue(){
        Map<String,Object> map = new HashMap<>();
        //绑定死信交换机
        map.put("x-dead-letter-exchange",DEAD_LETTER_EXCHANGE_NAME);
        //绑定的死信路由键
        map.put("x-dead-letter-routing-key",DEAD_LETTER_QUEUE_ROUTING_KEY_NAME);
        return QueueBuilder.durable(BI_QUEUE_NAME).withArguments(map).build();
    }

    /**
     * 申明死信队列
     * @return
     */
    @Bean
    public Queue deadLetterQueue(){
        return new Queue(DEAD_LETTER_QUEUE_NAME);
    }


    /**
     * 业务队列绑定到业务交换机
     * @return
     */
    @Bean
    public Binding businessBinding(){
        return BindingBuilder.bind(queue()).to(businessExchange()).with(BI_ROUTING_KEY_NAME);
    }

    /**
     * 死信队列绑定到死信交换机
     * @return
     */
    @Bean
    public Binding deadLetterBinding(){
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_QUEUE_ROUTING_KEY_NAME);
    }
}

