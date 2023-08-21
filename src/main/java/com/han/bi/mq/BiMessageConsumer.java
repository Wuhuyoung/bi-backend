package com.han.bi.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.han.bi.common.CommonConstants;
import com.han.bi.common.ErrorCode;
import com.han.bi.common.TaskStatus;
import com.han.bi.constant.UserConstant;
import com.han.bi.exception.BusinessException;
import com.han.bi.manager.AiManager;
import com.han.bi.model.entity.Chart;
import com.han.bi.model.entity.User;
import com.han.bi.service.ChartService;
import com.han.bi.service.UserService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class BiMessageConsumer {
    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Resource
    private UserService userService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitmqConfig.BI_QUEUE_NAME)
    public void receiveMessage(Message message, Channel channel) {
        String msg = new String(message.getBody());
        boolean ack = true;
        Exception exception = null;
        long chartId = -1;
        Chart chart = null;
        try {
            if (StringUtils.isBlank(msg)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
            }
            chartId = Long.parseLong(msg);
            chart = chartService.getById(chartId);

            if (chart == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表不存在");
            }

            // 构造用户输入
            StringBuilder userInput = getUserInput(chart);

            // 先修改图表状态为执行中，执行完成后修改为succeed，执行失败修改为failed
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            updateChart.setStatus(TaskStatus.RUNNING.getStatus());
            boolean updateRunning = chartService.updateById(updateChart);
            if (!updateRunning) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "chartId = " + chart.getId() + "，图表状态running修改失败");
            }
            // 调用AI对话接口
            String answer = aiManager.doChar(CommonConstants.BI_MODEL_ID, userInput.toString());
            if (answer == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
            }
            String[] splits = answer.split("&&&");
            if (splits.length != 3) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
            }
            String genChart = splits[1].trim();
            String genAnalysis = splits[2].trim();

            updateChart.setStatus(TaskStatus.SUCCEED.getStatus());
            updateChart.setGenChart(genChart);
            updateChart.setGenResult(genAnalysis);
            boolean updateSucceed = chartService.updateById(updateChart);
            if (!updateSucceed) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "chartId = " + chart.getId() + "，图表状态succeed修改失败");
            }
        } catch (Exception e) {
            ack = false;
            exception = e;
        }
        try {
            if (!ack){
                log.error("队列消费发生异常，chartId = {}, error msg:{}", chartId, exception.getMessage());
                /**
                 * void basicNack(long deliveryTag, boolean multiple, boolean requeue)
                 * 参数一：当前消息的唯一id
                 * 参数二：是否针对多条消息
                 * 参数三：是否从新入队列
                 */
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } else {
                // 调用成功，将数据库中调用次数 - 1，从 Redis 中获取次数
                Long userId = chart.getUserId();
                User loginUser = userService.getById(userId);
                String countStr = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
                if (countStr != null) {
                    User user = new User();
                    user.setId(loginUser.getId());
                    user.setLeftCount(Integer.parseInt(countStr));
                    userService.updateById(user);
                }
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("队列消息手动确认失败");
        }
    }

    @NotNull
    private StringBuilder getUserInput(Chart chart) {
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        String userGoal = chart.getGoal();
        if (StringUtils.isNotBlank(chart.getChartType())) { // 用户指定生成图表类型
            userGoal += "，请使用" + chart.getChartType();
        }
        userInput.append(userGoal).append("\n");
        userInput.append("数据：").append("\n");
        // 压缩后的数据
        userInput.append(chart.getChartData());
        return userInput;
    }
}
