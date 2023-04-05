package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCategoryServiceimpl implements CourseCategoryService {
    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        //调用mapper递归查询出分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);
        //找到每个节点的子节点，最终封装成List<CouseCategoryTreeDto>
        //先将List转成map，key就是节点id，value就是Dto对象，目的就是为了方便map获取结点，filter
        Map<String, CourseCategoryTreeDto> courseCategoryTreeDtoMap = courseCategoryTreeDtos.stream().
                filter(item -> !id.equals(item.getId())).
                collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        //这就是id对应节点的所有子结点集合
        List<CourseCategoryTreeDto> ans = new ArrayList<>();
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach((item -> {
            if(item.getParentid().equals(id)){
                ans.add(item);
            }
            //找到节点的父节点
            CourseCategoryTreeDto courseCategoryParent = courseCategoryTreeDtoMap.get(item.getParentid());
            if(courseCategoryParent != null){
                if(courseCategoryParent.getChildrenTreeNodes() == null){
                    courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                courseCategoryParent.getChildrenTreeNodes().add(item);
            }
        }));
        return ans;
    }
}
