package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

 /**
  * @description 媒资文件查询方法
  * @param pageParams 分页参数
  * @param queryMediaParamsDto 查询条件
  * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
  * @author Mr.M
  * @date 2022/9/10 8:57
 */
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

 /**
  *
  * @param companyId
  * @param localFilePath
  * @param uploadFileParamsDto 文件信息
  * @return
  */
 public UploadFileResultDto uploadFile(Long companyId, String localFilePath, UploadFileParamsDto uploadFileParamsDto);

 public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket_mediafiles, String objectName);

 /**
  * 检查文件是否在数据库已经存在
  * @param fileMd5
  * @return
  */
 public RestResponse<Boolean> checkfile(String fileMd5);

 public RestResponse<Boolean> checkchunk( String fileMd5,  int chunk);

 /**
  *
  * @param fileMd5
  * @param chunk 分块序号
  * @param localChunkFilePath
  * @return
  */
 public RestResponse uploadchunk( String fileMd5,  int chunk, String localChunkFilePath);

 /**
  *
  * @param companyId 机构id
  * @param fileMd5 文件md5值
  * @param chunkTotal  分块数量
  * @param uploadFileParamsDto  文件信息参数
  * @return
  */
 public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto);
}
