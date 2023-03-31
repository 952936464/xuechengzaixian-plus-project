package com.xuecheng.content.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 *dto数据传输对象
 */
@Data
@ToString
public class QueryCourseParamsDto {
    //审核状态
    private String auditStatus;
    //课程名称
    private String courseName;
    //发布状态
    private String publishStatus;

}
