package com.han.bi.model.vo;

import lombok.Data;

/**
 * AI 的返回结果
 */
@Data
public class AiResponse {
    private String genChart;
    private String genAnalysis;
    private Long chartId;
}
