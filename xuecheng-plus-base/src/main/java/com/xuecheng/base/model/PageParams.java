package com.xuecheng.base.model;

import lombok.Data;
import lombok.ToString;

/**
 * PageParams分页查询参数
 * toString输出日志方便
 * pageNo当前页数
 * pageSize每页显示的记录数
 */
@Data
@ToString
public class PageParams {

    private long pageNo=1L;

    private long pageSize = 30L;

    public PageParams(){};

    public PageParams(long pageNo, long pageSize){
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }
}
