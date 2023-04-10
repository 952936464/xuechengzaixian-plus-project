package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
 @Slf4j
 @Service
public class MediaFileServiceImpl implements MediaFileService {
  //普通文件，这是从nacos读取的文件
  @Value("${minio.bucket.files}")
  private String bucket_mediafiles;
  //视频文件
 @Value("${minio.bucket.videofiles}")
 private String bucket_video;
  @Autowired
 MediaFilesMapper mediaFilesMapper;
  @Autowired
 MinioClient minioClient;
  @Autowired
  MediaFileService mediaFileService;



 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();
  
  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }

 /**
  * 根据扩展名取出mimeType
  * @param extension
  * @return
  */
 private String getMimeType(String extension){
  if(extension == null){
   extension = "";
  }
  //根据扩展名取出mimeType
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".,mp4");
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
  if(extensionMatch != null){
   mimeType = extensionMatch.getMimeType();
  }
  return  mimeType;
 }

 /**
  * 将文件添加到Minio
  * @param bucket
  * @param localFilePath
  * @param objectName
  * @param mimeType 文件类型
  * @return
  */

 private Boolean addMediaFilesToMinIo(String bucket, String localFilePath, String objectName, String mimeType){

  try {
   UploadObjectArgs uploadObjectArgs  = UploadObjectArgs.builder()
           .bucket(bucket)//桶
           .filename(localFilePath)
           .object(objectName)
           .contentType(mimeType)
           .build();
   minioClient.uploadObject(uploadObjectArgs);
   log.debug("上传到minio成功，bucket:{},objectName:{}",bucket,objectName);
   return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件出错,bucket:{},objectName:{},错误信息{}",bucket,objectName,e.getMessage());
  }
  return false;
 }

 /**
  * 与前端约定上传到2023/04/10文件夹（例子）
  * @return
  */
 private String getDefaultFolderPath(){
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-","/") + "/";
  return folder;
 }

 /**
  * 根据文件获取其Md5值
  * @param file
  * @return
  */
 private String getFileMd5(File file){
  try(FileInputStream fileInputStream = new FileInputStream(file)){
   String fileMd5 = DigestUtils.md5DigestAsHex(fileInputStream);
   return fileMd5;
  }catch (Exception e){
   e.printStackTrace();
   return null;
  }
 }
 @Override

 public UploadFileResultDto uploadFile(Long companyId, String localFilePath, UploadFileParamsDto uploadFileParamsDto) {
  String filename = uploadFileParamsDto.getFilename();
  String extension = filename.substring(filename.lastIndexOf("."));
  //得到mimeType
  String mimeType = getMimeType(extension);
  String defaultFolderPath = getDefaultFolderPath();
  String fileMd5 = getFileMd5(new File(localFilePath));
  String objectName = defaultFolderPath + fileMd5 + extension;
  //上传文件到minio
  Boolean result = addMediaFilesToMinIo(bucket_mediafiles, localFilePath, objectName, mimeType);
  if(!result){
   XueChengPlusException.cast("上传文件失败");
  }
  MediaFiles mediaFiles = mediaFileService.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
  if(mediaFiles == null){
   XueChengPlusException.cast("文件上传后保存信息失败");
  }
  //准备返回对象
  UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
  BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
  return uploadFileResultDto;

 }
 @Transactional
 public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket,String objectName){
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if(mediaFiles == null){
   mediaFiles = new MediaFiles();
   BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
   mediaFiles.setId(fileMd5);
   mediaFiles.setCompanyId(companyId);
   mediaFiles.setBucket(bucket);
   mediaFiles.setFilePath(objectName);
   mediaFiles.setFileId(fileMd5);
   mediaFiles.setUrl("/" + bucket + "/" + objectName);
   mediaFiles.setCreateDate(LocalDateTime.now());
   mediaFiles.setStatus("1");
   mediaFiles.setAuditStatus("002003");
   int insert = mediaFilesMapper.insert(mediaFiles);
   if(insert <= 0){
    log.debug("文件向数据库保存失败,bucket:{},objectName:{}",bucket,objectName);
    return null;
   }
  }
  return mediaFiles;
 }

}
