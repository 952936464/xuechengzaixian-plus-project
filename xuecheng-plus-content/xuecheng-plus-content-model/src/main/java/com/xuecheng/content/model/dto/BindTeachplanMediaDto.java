package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 绑定媒资和课程计划的模型类
 */
@Data
@ApiModel(value = "BindTeachplanMediaDto", description = "教学计划-媒资绑定提交数据")
public class BindTeachplanMediaDto {
    private String mediaId;
    private String fileName;
    private Long teachplanId;
}
