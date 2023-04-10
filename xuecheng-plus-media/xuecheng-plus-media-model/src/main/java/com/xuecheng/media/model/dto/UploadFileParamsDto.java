package com.xuecheng.media.model.dto;

import lombok.Data;
import lombok.ToString;

/**
 * 文件信息
 */
@Data
@ToString
public class UploadFileParamsDto {
    private  String filename;

    private String fileType;

    private Long fileSize;

    private String tags;

    private String username;

    private String remark;

}
