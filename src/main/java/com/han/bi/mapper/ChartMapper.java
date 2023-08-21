package com.han.bi.mapper;

import com.han.bi.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author 86183
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2023-08-02 23:42:41
* @Entity com.han.bi.model.entity.Chart
*/
@Mapper
public interface ChartMapper extends BaseMapper<Chart> {

}




