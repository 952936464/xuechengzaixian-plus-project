package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service//需要放到spring容器
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto) {


        //详细进行分页查询的单元测试
        //拼装查询条件

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //todo:按课程发布状态查询
        queryWrapper.like(StringUtils.isNotBlank(courseParamsDto.getPublishStatus()),
                CourseBase::getStatus, courseParamsDto.getPublishStatus());
        //根据名称模糊查询
        queryWrapper.like(StringUtils.isNotBlank(courseParamsDto.getCourseName()),
                CourseBase::getName, courseParamsDto.getCourseName());
        queryWrapper.eq(StringUtils.isNotBlank(courseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus, courseParamsDto.getAuditStatus());
        //分页参数对象
        //创建page分页参数对象，参数：当前页码，每页记录数
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        Page<CourseBase> pageResult = courseBaseMapper.selectPage(page, queryWrapper);
        //数据列表
        List<CourseBase> items = pageResult.getRecords();
        long total = pageResult.getTotal();
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(items, total, pageParams.getPageNo(), pageParams.getPageSize());
        return  courseBasePageResult;
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(long companyId, AddCourseDto dto) {
        //参数的合法性校验

        //向课程基本表course_base写入数据
        CourseBase courseBaseDtoNew = new CourseBase();
        BeanUtils.copyProperties(dto, courseBaseDtoNew);
        courseBaseDtoNew.setCompanyId(companyId);
        courseBaseDtoNew.setCreateDate(LocalDateTime.now());
        courseBaseDtoNew.setAuditStatus("202002");
        courseBaseDtoNew.setStatus("203001");
        int insert = courseBaseMapper.insert(courseBaseDtoNew);
        if(insert <= 0){
            throw new RuntimeException("插入失败");
        }
        Long courseId = courseBaseDtoNew.getId();
        //向课程营销表course_market写入数据
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarketNew);
        courseMarketNew.setId(courseId);
        saveCourseMarket(courseMarketNew);

        //从数据库课程的详细信息，包括两部分
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);

        return courseBaseInfo;
    }

    /**
     *
     * @param courseMarketNew
     * @return 插入的条数
     */
    private int saveCourseMarket(CourseMarket courseMarketNew){
        int insert = 0;
        String charge = courseMarketNew.getCharge();
        //参数合法性校验
        if(StringUtils.isBlank(charge)){
            throw new RuntimeException("收费规则为空");
        }
        if(charge.equals("201001")){
            if(courseMarketNew.getPrice() == null || courseMarketNew.getPrice() <= 0){
                throw new RuntimeException("课程价格必须大于0");
            }
        }
        //从数据库查询营销信息，存在更新，不存在添加
        CourseMarket courseMarket = courseMarketMapper.selectById(courseMarketNew.getId());
        if(courseMarket == null){
            insert = courseMarketMapper.insert(courseMarketNew);
        }else{
            BeanUtils.copyProperties(courseMarketNew, courseMarket);
            insert = courseMarketMapper.updateById(courseMarket);
        }
        return insert;
    }
    private CourseBaseInfoDto getCourseBaseInfo(long courseId){
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase == null){
            return null;
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        if(courseMarket != null){
            BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);
        }

        CourseCategory courseCategory = courseCategoryMapper.selectById(courseId);
        if(courseCategory != null){
            BeanUtils.copyProperties(courseCategory,courseBaseInfoDto);
        }

        return courseBaseInfoDto;
    }
}
