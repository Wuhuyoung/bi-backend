package com.han.bi.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑请求
 *
 * @author <a href="https://github.com/Wuhuyoung">Wuhuyoung</a>
 * @from <a href="http://bi.wuhuyoung.top">DataMaster</a>
 */
@Data
public class ChartEditRequest implements Serializable {

    /**
     * 图表名称
     */
    private String name;

    /**
     * id
     */
    private Long id;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}