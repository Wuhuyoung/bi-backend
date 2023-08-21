package com.han.bi.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.han.bi.annotation.AuthCheck;
import com.han.bi.common.*;
import com.han.bi.constant.CommonConstant;
import com.han.bi.constant.UserConstant;
import com.han.bi.exception.BusinessException;
import com.han.bi.exception.ThrowUtils;
import com.han.bi.manager.AiManager;
import com.han.bi.manager.RedisLimiterManager;
import com.han.bi.model.dto.chart.*;
import com.han.bi.model.entity.Chart;
import com.han.bi.model.entity.User;
import com.han.bi.model.vo.AiResponse;
import com.han.bi.mq.BiMessageConsumer;
import com.han.bi.mq.BiMessageProducer;
import com.han.bi.service.ChartService;
import com.han.bi.service.UserService;
import com.han.bi.utils.ExcelUtils;
import com.han.bi.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RateIntervalUnit;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 *
 * @author <a href="https://github.com/Wuhuyoung">Wuhuyoung</a>
 * @from <a href="http://bi.wuhuyoung.top">DataMaster</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;


    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String name = chartQueryRequest.getName();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.like(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chart_type", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq("is_delete", false);

        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField == null ? "" : sortField.replaceAll("([A-Z])", "_$1").toLowerCase()); //将chartType转为chart_type
        return queryWrapper;
    }


    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen_async")
    public BaseResponse<AiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 判断调用次数是否足够
        String strCount = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
        int count = 0;
        if (strCount == null) {
            count = loginUser.getLeftCount();
        } else {
            count = Integer.parseInt(strCount);
        }
        if (count <= 0) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "剩余调用次数为 0");
        }

        if (multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传数据文件");
        }
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        ThrowUtils.throwIf(goal.length() > 200, ErrorCode.PARAMS_ERROR, "分析目标不得超过200字");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,
                ErrorCode.PARAMS_ERROR, "名称过长");
        if (StringUtils.isNotBlank(name)) {
            name = name.trim();
        }
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long TEN_MB = 10 * 1024 * 1024L;
        ThrowUtils.throwIf(size > TEN_MB, ErrorCode.PARAMS_ERROR, "文件大小超过10MB");
        // 校验文件后缀名
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "不支持该文件格式");

        // 限流操作，限制每个用户1秒只能执行该方法2次
        redisLimiterManager.doLimit("genChartByAi_" + loginUser.getId(), 2, 1, RateIntervalUnit.SECONDS);

        // 扣减使用次数
        stringRedisTemplate.opsForValue().decrement(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());

        /*  分析需求：
            分析网站用户趋势
            数据：
            日期,用户数
            1号,15
            2号,20
            3号,25
            4号,25  */
        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) { // 用户指定生成图表类型
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("数据：").append("\n");
        // 压缩后的数据
        String scvData = ExcelUtils.excelToCsv(multipartFile, suffix); // 将 excel 转换为 csv
        userInput.append(scvData);

        // 保存到数据库
        Chart chart = new Chart();
        chart.setUserId(loginUser.getId());
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(scvData);
        chart.setChartType(chartType);
        chart.setStatus(TaskStatus.WAIT.getStatus());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 异步执行 智能分析操作
        try {
            CompletableFuture.runAsync(() -> { // 自定义线程池提交
                // 先修改图表状态为执行中，执行完成后修改为succeed，执行失败修改为failed
                Chart updateChart = new Chart();
                updateChart.setId(chart.getId());
                updateChart.setStatus(TaskStatus.RUNNING.getStatus());
                boolean updateRunning = chartService.updateById(updateChart);
                if (!updateRunning) {
                    handleGenChartError(chart.getId(), "图表状态running修改失败", loginUser);
                    return;
                }
                // 调用AI对话接口
                String answer = aiManager.doChar(CommonConstants.BI_MODEL_ID, userInput.toString());
                if (answer == null) {
                    handleGenChartError(chart.getId(), "AI 生成错误", loginUser);
                    return;
                }
                String[] splits = answer.split("&&&");
                if (splits.length != 3) {
                    handleGenChartError(chart.getId(), "AI 生成错误", loginUser);
                    return;
                }
                String genChart = splits[1].trim();
                String genAnalysis = splits[2].trim();

                updateChart.setStatus(TaskStatus.SUCCEED.getStatus());
                updateChart.setGenChart(genChart);
                updateChart.setGenResult(genAnalysis);
                boolean updateSucceed = chartService.updateById(updateChart);
                if (!updateSucceed) {
                    handleGenChartError(chart.getId(), "图表状态succeed修改失败", loginUser);
                    return;
                }

                // 调用成功，将数据库中调用次数 - 1，从 Redis 中获取次数
                String countStr = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
                if (countStr != null) {
                    User user = new User();
                    user.setId(loginUser.getId());
                    user.setLeftCount(Integer.parseInt(countStr));
                    userService.updateById(user);
                }
            }, threadPoolExecutor);
        } catch (RejectedExecutionException e) {
            // 当任务数超过线程池任务队列且没有空闲线程，会抛出拒绝异常
            handleGenChartError(chart.getId(), "任务队列拒绝执行", loginUser);
        } catch (Exception e) {
            handleGenChartError(chart.getId(), "执行失败", loginUser);
        }

        AiResponse aiResponse = new AiResponse();
        aiResponse.setChartId(chart.getId());
        return ResultUtils.success(aiResponse);
    }

    private void handleGenChartError(Long id, String message, User loginUser) {
        Chart failedChart = new Chart();
        failedChart.setId(id);
        // 将图表状态修改为failed
        failedChart.setStatus(TaskStatus.FAILED.getStatus());
        log.error(message + ", chartId=" + id);
        boolean updateResult = chartService.updateById(failedChart);
        if (!updateResult) {
            log.error("将图表状态修改为failed失败");
        }
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

    /**
     * 智能分析（异步消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen_async/mq")
    public BaseResponse<AiResponse> genChartByAiAsyncMQ(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        // 判断调用次数是否足够
        String strCount = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
        int count = 0;
        if (strCount == null) {
            count = loginUser.getLeftCount();
        } else {
            count = Integer.parseInt(strCount);
        }
        if (count <= 0) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "剩余调用次数为 0");
        }

        if (multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传数据文件");
        }
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        ThrowUtils.throwIf(goal.length() > 200, ErrorCode.PARAMS_ERROR, "分析目标不得超过200字");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,
                ErrorCode.PARAMS_ERROR, "名称过长");
        if (StringUtils.isNotBlank(name)) {
            name = name.trim();
        }
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long TEN_MB = 10 * 1024 * 1024L;
        ThrowUtils.throwIf(size > TEN_MB, ErrorCode.PARAMS_ERROR, "文件大小超过10MB");
        // 校验文件后缀名
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "不支持该文件格式");

        // 限流操作，限制每个用户1秒只能执行该方法2次
        redisLimiterManager.doLimit("genChartByAi_" + loginUser.getId(), 2, 1, RateIntervalUnit.SECONDS);

        // 扣减使用次数
        stringRedisTemplate.opsForValue().decrement(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());

        // 压缩后的数据
        String scvData = ExcelUtils.excelToCsv(multipartFile, suffix); // 将 excel 转换为 csv

        // 保存到数据库
        Chart chart = new Chart();
        chart.setUserId(loginUser.getId());
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(scvData);
        chart.setChartType(chartType);
        chart.setStatus(TaskStatus.WAIT.getStatus());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 异步执行 向消息队列提交
        Long id = chart.getId();
        try {
            biMessageProducer.sendMessage(String.valueOf(id));
        } catch (Exception e) {
            Chart failedChart = new Chart();
            failedChart.setId(loginUser.getId());
            failedChart.setStatus(TaskStatus.FAILED.getStatus());
            chartService.updateById(failedChart);

            // 提交消息失败，将使用次数 + 1
            String countStr = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
            if (countStr != null && Integer.parseInt(countStr) < 50) {
                stringRedisTemplate.opsForValue().increment(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
                User user = new User();
                user.setId(loginUser.getId());
                user.setLeftCount(Integer.parseInt(countStr) + 1);
                userService.updateById(user);
            }
            e.printStackTrace();
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
        }

        AiResponse aiResponse = new AiResponse();
        aiResponse.setChartId(id);
        return ResultUtils.success(aiResponse);
    }


    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<AiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 判断调用次数是否足够
        String strCount = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
        int count = 0;
        if (strCount == null) {
            count = loginUser.getLeftCount();
        } else {
            count = Integer.parseInt(strCount);
        }
        if (count <= 0) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "剩余调用次数为 0");
        }

        // 限流操作，限制每个用户1秒只能执行该方法1次
        redisLimiterManager.doLimit("retryGenChartByAi_" + loginUser.getId(), 1, 1, RateIntervalUnit.SECONDS);

        if (multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请上传数据文件");
        }
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 参数校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        ThrowUtils.throwIf(goal.length() > 200, ErrorCode.PARAMS_ERROR, "分析目标不得超过200字");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100,
                ErrorCode.PARAMS_ERROR, "名称过长");
        if (StringUtils.isNotBlank(name)) {
            name = name.trim();
        }
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 校验文件大小
        final long TEN_MB = 10 * 1024 * 1024L;
        ThrowUtils.throwIf(size > TEN_MB, ErrorCode.PARAMS_ERROR, "文件大小超过10MB");
        // 校验文件后缀名
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "不支持该文件格式");

        // 扣减使用次数
        stringRedisTemplate.opsForValue().decrement(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());

        /*  分析需求：
            分析网站用户趋势
            数据：
            日期,用户数
            1号,15
            2号,20
            3号,25
            4号,25  */
        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) { // 用户指定生成图表类型
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("数据：").append("\n");
        // 压缩后的数据
        String scvData = ExcelUtils.excelToCsv(multipartFile, suffix); // 将 excel 转换为 csv
        userInput.append(scvData);
        String genChart = null;
        String genAnalysis = null;
        Chart chart = null;

        try {
            // 调用AI对话接口
            String answer = aiManager.doChar(CommonConstants.BI_MODEL_ID, userInput.toString());
            if (answer == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
            }

            String[] splits = answer.split("&&&");
            if (splits.length != 3) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成错误");
            }
            genChart = splits[1].trim();
            genAnalysis = splits[2].trim();
            // 保存到数据库
            chart = new Chart();
            chart.setUserId(loginUser.getId());
            chart.setName(name);
            chart.setGoal(goal);
            chart.setChartData(scvData);
            chart.setChartType(chartType);
            chart.setGenChart(genChart);
            chart.setGenResult(genAnalysis);
            chart.setStatus(TaskStatus.SUCCEED.getStatus()); // 将状态修改为成功
            boolean saveResult = chartService.save(chart);
            ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        } catch (BusinessException e) {
            chart.setId(loginUser.getId());
            chart.setStatus(TaskStatus.FAILED.getStatus());
            chartService.updateById(chart); // 将状态修改为失败
            // 调用失败，将使用次数 + 1
            String countStr = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
            if (countStr != null && Integer.parseInt(countStr) < 50) {
                stringRedisTemplate.opsForValue().increment(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
                User user = new User();
                user.setId(loginUser.getId());
                user.setLeftCount(Integer.parseInt(countStr) + 1);
                userService.updateById(user);
            }
            throw e;
        }

        // 将数据库中调用次数 - 1
        String countStr = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
        if (countStr != null) {
            User user = new User();
            user.setId(loginUser.getId());
            user.setLeftCount(Integer.parseInt(countStr));
            userService.updateById(user);
        }

        AiResponse aiResponse = new AiResponse();
        aiResponse.setGenChart(genChart);
        aiResponse.setGenAnalysis(genAnalysis);
        aiResponse.setChartId(chart.getId());
        return ResultUtils.success(aiResponse);
    }

    /**
     * 生成失败后重新提交（异步）
     *
     * @param chartId
     * @param request
     * @return
     */
    @PostMapping("/retry_async")
    public BaseResponse<AiResponse> retryGenChartByAiAsync(Long chartId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        // 判断调用次数是否足够
        String strCount = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
        int count = 0;
        if (strCount == null) {
            count = loginUser.getLeftCount();
        } else {
            count = Integer.parseInt(strCount);
        }
        if (count <= 0) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "剩余调用次数为 0");
        }

        // 限流操作，限制每个用户1秒只能执行该方法1次
        redisLimiterManager.doLimit("retryGenChartByAi_" + loginUser.getId(), 1, 1, RateIntervalUnit.SECONDS);

        // 扣减使用次数
        stringRedisTemplate.opsForValue().decrement(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());

        // 异步执行 智能分析操作
        try {
            CompletableFuture.runAsync(() -> { // 自定义线程池提交
                // 先修改图表状态为执行中，执行完成后修改为succeed，执行失败修改为failed
                Chart updateChart = new Chart();
                updateChart.setId(chartId);
                updateChart.setStatus(TaskStatus.RUNNING.getStatus());
                boolean updateRunning = chartService.updateById(updateChart);
                if (!updateRunning) {
                    handleGenChartError(chartId, "图表状态running修改失败", loginUser);
                    return;
                }
                // 重新构造用户输入
                StringBuilder userInput = new StringBuilder();
                Chart chart = chartService.getById(chartId);
                userInput.append("分析需求：").append("\n");
                String userGoal = chart.getGoal();
                if (StringUtils.isNotBlank(chart.getChartType())) { // 用户指定生成图表类型
                    userGoal += "，请使用" + chart.getChartType();
                }
                userInput.append(userGoal).append("\n");
                userInput.append("数据：").append("\n");
                // 压缩后的数据
                userInput.append(chart.getChartData());

                // 调用AI对话接口
                String answer = aiManager.doChar(CommonConstants.BI_MODEL_ID, userInput.toString());
                if (answer == null) {
                    handleGenChartError(chart.getId(), "AI 生成错误", loginUser);
                    return;
                }
                String[] splits = answer.split("&&&");
                if (splits.length != 3) {
                    handleGenChartError(chart.getId(), "AI 生成错误", loginUser);
                    return;
                }
                String genChart = splits[1].trim();
                String genAnalysis = splits[2].trim();

                updateChart.setStatus(TaskStatus.SUCCEED.getStatus());
                updateChart.setGenChart(genChart);
                updateChart.setGenResult(genAnalysis);
                boolean updateSucceed = chartService.updateById(updateChart);
                if (!updateSucceed) {
                    handleGenChartError(chart.getId(), "图表状态succeed修改失败", loginUser);
                    return;
                }
                // 调用成功，将数据库中调用次数 - 1，从 Redis 中获取次数
                String countStr = stringRedisTemplate.opsForValue().get(UserConstant.USER_LEFT_COUNT_KEY + loginUser.getUserAccount());
                if (countStr != null) {
                    User user = new User();
                    user.setId(loginUser.getId());
                    user.setLeftCount(Integer.parseInt(countStr));
                    userService.updateById(user);
                }
            }, threadPoolExecutor);
        } catch (RejectedExecutionException e) {
            // 当任务数超过线程池任务队列且没有空闲线程，会抛出拒绝异常
            handleGenChartError(chartId, "任务队列拒绝执行", loginUser);
        } catch (Exception e) {
            handleGenChartError(chartId, "执行失败", loginUser);
        }
        AiResponse aiResponse = new AiResponse();
        aiResponse.setChartId(chartId);
        return ResultUtils.success(aiResponse);
    }

}
