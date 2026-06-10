package com.smartretail.mbc.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {

    private List<T> records;

    private Long total;

    private Long pageNum;

    private Long pageSize;

    public static <T> PageResult<T> of(List<T> records, Long total, Long pageNum, Long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        return result;
    }
}
