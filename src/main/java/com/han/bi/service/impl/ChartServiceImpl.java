package com.han.bi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.han.bi.model.entity.Chart;
import com.han.bi.service.ChartService;
import com.han.bi.mapper.ChartMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
* @author 86183
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2023-08-02 23:42:41
*/
@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

}




