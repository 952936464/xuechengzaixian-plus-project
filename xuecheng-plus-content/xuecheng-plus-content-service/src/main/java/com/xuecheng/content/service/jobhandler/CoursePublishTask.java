package com.xuecheng.content.service.jobhandler;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 课程发布的任务类
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception{
        // 分片参数

        int shardIndex = XxlJobHelper.getShardIndex();//执行器序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal();//执行器总数
        process(shardIndex, shardTotal, "course_publish",30,60);
    }

    @Override
    public boolean execute(MqMessage mqMessage) {
        //拿到课程id
        long courseId = Long.parseLong(mqMessage.getBusinessKey1());
        //向elasticsearch写索引数据

        //向redis写缓存

        //课程静态化上传到minio
        generateCourseHtml(mqMessage, courseId);

        return true;
    }
    private void generateCourseHtml(MqMessage mqMessage, long courseId){
        //消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //做任务幂等性处理
        //查询数据库取出该阶段的执行状态
        int stageOne = mqMessageService.getStageOne(courseId);
        if(stageOne > 0){
            log.debug("课程已经完成静态化处理，无需处理");
            return;
        }
        //开始进行课程的静态化
        //..任务处理完成写任务状态为完成


    }
    private void saveCourseIndex(MqMessage mqMessage, long courseId){
        //消息id
        Long taskId = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();

        //做任务幂等性处理
        //查询数据库取出该阶段的执行状态
        int stageTwo = mqMessageService.getStageTwo(courseId);
        if(stageTwo > 0){
            log.debug("课程已经完成静态化处理，无需处理");
            return;
        }
        //开始进行课程的静态化
        //..任务处理完成写任务状态为完成


    }
}
