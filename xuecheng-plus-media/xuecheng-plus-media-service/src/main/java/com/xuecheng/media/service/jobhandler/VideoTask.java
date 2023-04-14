package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class VideoTask {
    @Autowired
    MediaProcessMapper mediaProcessMapper;
    @Autowired
    MediaFileProcessService mediaFileProcessService;
    @Autowired
    MediaFileService mediaFileService;
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;
    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();//执行器序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal();//执行器总数
        //查询待处理任务
        int cpuCores = Runtime.getRuntime().availableProcessors();
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, cpuCores);
        //创建多线程处理任务
        int size = mediaProcesses.size();
        log.debug("取到的任务数是：{}",size);
        if(size <= 0){
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcesses.forEach(item -> {
            //将任务加入线程池
            executorService.execute(() ->{
                //开启任务
                try {
                    Long taskId = item.getId();
                    boolean b = mediaFileProcessService.startTask(taskId);
                    if(!b){
                        log.debug("抢占任务失败，任务id：{}",taskId);
                        return;
                    }
                    //下载minio视频到本地
                    File file = mediaFileService.downLoadFileFormMinIo(item.getBucket(), item.getFilePath());
                    if(file == null){
                        log.debug("下载视频出错，任务id：{},bucket:{},objectName:{}",taskId,item.getBucket(),item.getFilePath());
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", item.getFileId(), null, "下载视频到本地失败");
                        return;
                    }
                    //源avi视频的路径
                    String video_path = file.getAbsolutePath();
                    String fileId = item.getFileId();
                    //转换后mp4文件的名称
                    String mp4_name = fileId + ".mp4";
                    //转换后mp4文件的路径
                    File mp4File = null;
                    try{
                        mp4File = File.createTempFile("minio," ,".mp4");
                    }catch (IOException e){
                        log.debug("创建临时文件异常,{}",e.getMessage());
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", item.getFileId(), null, "创建临时文件异常");
                        return;
                    }
                    String mp4_path = mp4File.getAbsolutePath();
                    //创建工具类对象
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath,video_path,mp4_name,mp4_path);
                    //开始视频转换，成功将返回success
                    String ans = videoUtil.generateMp4();
                    if(!ans.equals("success")){
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", item.getFileId(), null, "视频转码失败");
                    }
                    //上传到minio
                    Boolean b1 = mediaFileService.addMediaFilesToMinIo(item.getBucket(), mp4_path, item.getFilePath(), "video/mp4");
                    if(!b1){
                        log.debug("上传MP4到minio失败,taskId:{}",taskId);
                        //保存任务处理失败的结果
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "3", item.getFileId(), null, "上传到MP4到minio失败");
                    }
                    String url = getFilePathByMd5(item.getFileId(), "mp4");
                    //保存任务的处理结果
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", item.getFileId(), url, "保存任务成功");
                } finally {
                    countDownLatch.countDown();
                }
            });
        });
        //设置最大等待时间，最多等待30分钟，防止前面的异常导致countDown没有正常减
        countDownLatch.await(30, TimeUnit.MINUTES);

    }
    private String getFilePathByMd5(String fileMd5, String fileExt){
        //分块存储路径为MD5前两位为两个目录，chunk存储分块文件
        //根据md5得到分块文件路径
        return fileMd5.substring(0,1) + "/" + fileMd5.substring(1,2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
}
