package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 @Autowired
 MediaProcessMapper mediaProcessMapper;


 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

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
  *
  * @param extension
  * @return
  */
 private String getMimeType(String extension) {
  if (extension == null) {
   extension = "";
  }
  //根据扩展名取出mimeType
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".,mp4");
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
  if (extensionMatch != null) {
   mimeType = extensionMatch.getMimeType();
  }
  return mimeType;
 }

 /**
  * 将文件添加到Minio
  *
  * @param bucket
  * @param localFilePath
  * @param objectName
  * @param mimeType      文件类型
  * @return
  */

 public Boolean addMediaFilesToMinIo(String bucket, String localFilePath, String objectName, String mimeType) {

  try {
   UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
           .bucket(bucket)//桶
           .filename(localFilePath)
           .object(objectName)
           .contentType(mimeType)
           .build();
   minioClient.uploadObject(uploadObjectArgs);
   log.debug("上传到minio成功，bucket:{},objectName:{}", bucket, objectName);
   return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件出错,bucket:{},objectName:{},错误信息{}", bucket, objectName, e.getMessage());
  }
  return false;
 }

 /**
  * 与前端约定上传到2023/04/10文件夹（例子）
  *
  * @return
  */
 private String getDefaultFolderPath() {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/") + "/";
  return folder;
 }

 /**
  * 根据文件获取其Md5值
  *
  * @param file
  * @return
  */
 private String getFileMd5(File file) {
  try (FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5DigestAsHex(fileInputStream);
   return fileMd5;
  } catch (Exception e) {
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
  if (!result) {
   XueChengPlusException.cast("上传文件失败");
  }
  MediaFiles mediaFiles = mediaFileService.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_mediafiles, objectName);
  if (mediaFiles == null) {
   XueChengPlusException.cast("文件上传后保存信息失败");
  }
  //准备返回对象
  UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
  BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
  return uploadFileResultDto;

 }

 @Transactional
 public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if (mediaFiles == null) {
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
   addWaitingTask(mediaFiles);
   if (insert <= 0) {
    log.debug("文件向数据库保存失败,bucket:{},objectName:{}", bucket, objectName);
    return null;
   }
  }
  return mediaFiles;
 }

 @Override
 public RestResponse<Boolean> checkfile(String fileMd5) {
  //先查数据库
  MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
  if (mediaFiles != null) {
   String bucket = mediaFiles.getBucket();
   //objectname
   String filePath = mediaFiles.getFilePath();
   //如果数据库存在查询minio
   GetObjectArgs getObjectArgs = GetObjectArgs.builder()
           .bucket(bucket)
           .object(filePath)
           .build();
   try {
    GetObjectResponse inputStream = minioClient.getObject(getObjectArgs);
    if (inputStream != null) {
     //文件已经存在
     return RestResponse.success(true);
    }
   } catch (Exception e) {
    e.printStackTrace();
   }
  }
  return RestResponse.success(false);
 }

 @Override
 public RestResponse<Boolean> checkchunk(String fileMd5, int chunk) {
  //分块存储路径为MD5前两位为两个目录，chunk存储分块文件
  //根据md5得到分块文件路径
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);

  //如果数据库存在查询minio
  GetObjectArgs getObjectArgs = GetObjectArgs.builder()
          .bucket(bucket_video)
          .object(chunkFileFolderPath + chunk)
          .build();
  try {
   FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
   if (inputStream != null) {
    //序号为chunk的文件存在
    return RestResponse.success(true);
   }
  } catch (Exception e) {
   e.printStackTrace();
  }
  return RestResponse.success(false);
 }

 @Override
 public RestResponse uploadchunk(String fileMd5, int chunk, String localChunkFilePath) {
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  //上传到minio的文件路径
  String chunkFilePath = chunkFileFolderPath + chunk;
  //存在mioio中没有扩展名
  String mimeType = getMimeType(null);
  Boolean success = addMediaFilesToMinIo(bucket_video, localChunkFilePath, chunkFilePath, mimeType);
  if(!success){
   return RestResponse.validfail(false, "上传文件块{}失败");
  }
  return RestResponse.success(true);
 }

 /**
  *
  * @param companyId 机构id
  * @param fileMd5 文件md5值
  * @param chunkTotal  分块数量
  * @param uploadFileParamsDto  文件信息参数
  * objectName :这是带路径的名字
  * @return
  */
 @Override
 public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
  //找到分块文件，调用minio的sdk进行文件合并
  List<ComposeSource> sources = new ArrayList<>();
  String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
  for(int i = 0; i < chunkTotal; ++i){
    //指定文件的信息
    ComposeSource composeSource = ComposeSource.builder().bucket(bucket_video).object(chunkFileFolderPath + i).build();
    sources.add(composeSource);
   }
   //指定合并后的objectName信息
  String filename = uploadFileParamsDto.getFilename();
  String fileExt = filename.substring(filename.lastIndexOf("."));
  String objectName = getFilePathByMd5(fileMd5, fileExt);
  ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
          .bucket(bucket_video)
          .object(objectName)
          .sources(sources)
          .build();
  try {
   minioClient.composeObject(composeObjectArgs);
  } catch (Exception e) {
   e.printStackTrace();
   log.debug("合并文件出错,bucket:{},objectName:{},错误信息：{}",bucket_video, objectName, e.getMessage());
   return RestResponse.validfail(false, "合并文件异常");
  }
  //校验合并后的文件和源文件是否一致，视频上传才成功
  //先下载文件
  File file = downLoadFileFormMinIo(bucket_video, objectName);
  //存入文件的大小，因为前端没有传过来文件的大小
  uploadFileParamsDto.setFileSize(file.length());
  //放到括号里，tyr执行完毕后流自动关闭
  try(FileInputStream fileInputStream = new FileInputStream(file)){
   String mergeMd5 = DigestUtils.md5DigestAsHex(fileInputStream);
   if(!fileMd5.equals(mergeMd5)){
    log.error("校验合并文件md5不一致,原始文件：{}，合并文件：{}",fileMd5, mergeMd5);
    return RestResponse.validfail(false,"文件校验失败");
   }
  }catch (Exception e){
   e.printStackTrace();
   return RestResponse.validfail(false,"文件校验失败");
  }
  //将文件信息入库,//todo 非代理对象处理事务会出问题所以要让代理对象调用
  MediaFiles mediaFiles = mediaFileService.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_video, objectName);
  if(mediaFiles == null){
   return RestResponse.validfail(false, "文件入库失败");
  }
  //清理分块文件
  String fileFolderPath = getChunkFileFolderPath(fileMd5);
  clearChunkFiles(fileFolderPath, chunkTotal);
  return RestResponse.success(true);
 }

 /**
  *根据md5获取分块文件在minio中的位置
  * @param fileMd5
  * @return
  */
 private String getChunkFileFolderPath(String fileMd5){
  //分块存储路径为MD5前两位为两个目录，chunk存储分块文件
  //根据md5得到分块文件路径
  return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + fileMd5 + "/" + "chunk" + "/";
 }

 /**
  *
  * @param fileMd5 文件存储在的位置
  * @param fileExt
  * @return
  */
 private String getFilePathByMd5(String fileMd5, String fileExt){
  //分块存储路径为MD5前两位为两个目录，chunk存储分块文件
  //根据md5得到分块文件路径
  return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
 }
 public File downLoadFileFormMinIo(String bucket, String objectName) {
  File minioFile = null;
  FileOutputStream outputStream = null;
  try {
   InputStream stream = minioClient.getObject(GetObjectArgs.builder()
           .bucket(bucket)
           .object(objectName)
           .build());
   minioFile = File.createTempFile("minio", ".merge");
   outputStream = new FileOutputStream(minioFile);
   IOUtils.copy(stream, outputStream);
   return minioFile;
  } catch (Exception e) {
   e.printStackTrace();
  }finally {
   if(outputStream != null){
    try {
     outputStream.close();
    }catch (Exception e){
     e.printStackTrace();
    }
   }
  }
  return  null;
 }

 /**
  *
  * @param mediaFiles 文件
  *                   向等待表添加数据
  */
 private void addWaitingTask(MediaFiles mediaFiles){
  String filename = mediaFiles.getFilename();
  //文件扩展名
  String extension = filename.substring(filename.lastIndexOf("."));
  //根据文件扩展名获取mimetype
  String mimeType = getMimeType(extension);
  if(mimeType.equals("video/x-msvideo")){//如果是avi视频
   MediaProcess mediaProcess = new MediaProcess();
   BeanUtils.copyProperties(mediaFiles, mediaProcess);
   //状态是待处理
   mediaProcess.setStatus("1");
   mediaProcess.setCreateDate(LocalDateTime.now());
   mediaProcess.setFailCount(0);//失败次数默认为0
   mediaProcess.setUrl(null);
   mediaProcessMapper.insert(mediaProcess);
  }


 }

 /**
  *
  * @param chunkFileFolderPath 分块文件路径
  * @param chunkTotal  分块文件总数
  */
 private void clearChunkFiles(String chunkFileFolderPath, int chunkTotal){
   Iterable<DeleteObject> objects = Stream.iterate(0, i -> ++i).limit(chunkTotal)
           .map(i -> new DeleteObject(chunkFileFolderPath + i)).collect(Collectors.toList());
   RemoveObjectsArgs removeObjectArgs = RemoveObjectsArgs.builder().bucket(bucket_video).objects(objects).build();
  Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectArgs);
  results.forEach(item ->{
   try {
    DeleteError deleteError = item.get();
   } catch (Exception e) {
    e.printStackTrace();
   }
  });
 }
 }






