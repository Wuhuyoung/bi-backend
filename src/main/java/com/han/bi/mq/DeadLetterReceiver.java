package com.han.bi.mq;

import com.han.bi.common.TaskStatus;
import com.han.bi.constant.UserConstant;
import com.han.bi.model.entity.Chart;
import com.han.bi.model.entity.User;
import com.han.bi.service.ChartService;
import com.han.bi.service.UserService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;


/**
 * 死信队列的消费者
 */
@Slf4j
@Component
public class DeadLetterReceiver {
    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 监听业务队列
     * @param msg
     */
    @RabbitListener(queues = RabbitmqConfig.DEAD_LETTER_QUEUE_NAME)
    public void queue(Message msg, Channel channel) throws IOException {
        String str = new String(msg.getBody());
        log.info("死信队列接受到消息【{}】",str);
        String message = new String(msg.getBody());
        if (message == null) {
            channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        long chartId = Long.parseLong(message);
        handleGenChartError(chartId); // 使用死信队列来处理异常情况，将图表生成任务置为失败
        channel.basicAck(msg.getMessageProperties().getDeliveryTag(), false);
        log.info("死信消息properties：{}", msg.getMessageProperties());
    }

    private void handleGenChartError(Long id) {
        Chart failedChart = new Chart();
        failedChart.setId(id);
        // 将图表状态修改为failed
        failedChart.setStatus(TaskStatus.FAILED.getStatus());
        boolean updateResult = chartService.updateById(failedChart);
        if (!updateResult) {
            log.error("将图表状态修改为failed失败");
        }
        Chart chart = chartService.getById(id);
        Long userId = chart.getUserId();
        User loginUser = userService.getById(userId);
        // 调用失败，将使用次数 + 1
        String countStr = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
        if (countStr != null && Integer.parseInt(countStr) < 50) {
            stringRedisTemplate.opsForValue().increment(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
            User user = new User();
            user.setId(loginUser.getId());
            user.setLeftCount(Integer.parseInt(countStr) + 1);
            userService.updateById(user);
        }
    }
}

