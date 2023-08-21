package com.han.bi.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.han.bi.common.ErrorCode;
import com.han.bi.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel 工具类
 */
@Slf4j
public class ExcelUtils {
    /**
     * excel 转 csv
     *
     * @param multipartFile
     * @param suffix
     * @return
     */
    public static String excelToCsv(MultipartFile multipartFile, String suffix) {
//        File file = null;
//        try {
//            file = ResourceUtils.getFile("classpath:test_excel.xlsx");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        List<Map<Integer, String>> list = null;
        // 使用开源库 easyExcel 读取 excel
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(getFileType(suffix))
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("表格读取错误", e);
        }
        if (CollUtil.isEmpty(list)) {
            return "";
        }
        // 转换为 csv
        StringBuilder res = new StringBuilder();
        // 读取表头
        LinkedHashMap<Integer, String> headMap = (LinkedHashMap<Integer, String>) list.get(0);
        List<String> headList = headMap.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
        res.append(StringUtils.join(headList, ",")).append("\n");
        // 读取数据
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap<Integer, String>) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
            res.append(StringUtils.join(dataList, ",")).append("\n");
        }
        return res.toString();
    }

    private static ExcelTypeEnum getFileType(String suffix) {
        // ExcelTypeEnum.XLSX
        if ("xlsx".equals(suffix)) {
            return ExcelTypeEnum.XLSX;
        } else if ("xls".equals(suffix)) {
            return ExcelTypeEnum.XLS;
        } else if ("csv".equals(suffix)) {
            return ExcelTypeEnum.CSV;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件格式不支持");
    }
}
