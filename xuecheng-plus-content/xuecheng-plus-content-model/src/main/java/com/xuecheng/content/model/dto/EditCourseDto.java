package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Data
public class EditCourseDto extends AddCourseDto{
    @NotEmpty(message = "课程id不为空")
    @ApiModelProperty(value = "课程id", required = true)
    long id;
}
