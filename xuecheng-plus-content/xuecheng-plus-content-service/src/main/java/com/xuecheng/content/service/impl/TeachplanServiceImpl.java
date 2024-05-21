package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.exception.XueChengPlusException2;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.apache.ibatis.annotations.Case;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;

    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        return teachplanMapper.selectTreeNodes(courseId);
    }

    @Transactional
    @Override
    public void saveTeachplan(SaveTeachplanDto teachplanDto) {

        //课程计划id
        Long id = teachplanDto.getId();
        //修改课程计划
        if(id!=null){
            Teachplan teachplan = teachplanMapper.selectById(id);
            BeanUtils.copyProperties(teachplanDto,teachplan);
            teachplanMapper.updateById(teachplan);
        }else{
            //取出同父同级别的课程计划数量
            int count = getTeachplanCount(teachplanDto.getCourseId(), teachplanDto.getParentid());
            Teachplan teachplanNew = new Teachplan();
            //设置排序号
            teachplanNew.setOrderby(count+1);
            BeanUtils.copyProperties(teachplanDto,teachplanNew);

            teachplanMapper.insert(teachplanNew);

        }

    }

    @Transactional
    @Override
    public void deleteTeachplan(Long teachplanId) {
        // select teachplan to be deleted
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        // if the teachplan is null
        if (teachplan == null) {
            XueChengPlusException.cast("课程不存在");
        }

        Long courseId = teachplan.getCourseId();
        Long parentid = teachplan.getParentid();

        // if the teachplan is root
        if (parentid == 0) {
            Long id = teachplan.getId();
            // get the number of sections under the root
            int count = getTeachplanCount(courseId, id);

            if (count > 0) {
                XueChengPlusException2.cast("120409", "课程计划信息还有子级信息，无法操作");
            }
            teachplanMapper.deleteById(id);
        } else {
            // select the media of the teachplan
            LambdaQueryWrapper<TeachplanMedia> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TeachplanMedia::getTeachplanId,teachplanId);
            TeachplanMedia teachplanMedia = teachplanMediaMapper.selectOne(queryWrapper);

            // delete if there is associated media
            if (teachplanMedia != null) {
                teachplanMediaMapper.deleteById(teachplanMedia.getId());
            }

            teachplanMapper.deleteById(teachplanId);
        }
    }

    @Transactional
    @Override
    public void moveTeachplan(String movement, Long teachplanId) {
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        Integer orderby = teachplan.getOrderby();

        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        Teachplan targetTeachplan;
        switch (movement) {
            case "moveup":
                // the teachplan cannot move upwards if it is the first one
                if (orderby == 1)
                    break;


                queryWrapper.eq(Teachplan::getParentid, teachplan.getParentid());
                queryWrapper.eq(Teachplan::getOrderby, orderby - 1);
                targetTeachplan = teachplanMapper.selectOne(queryWrapper);

                if (targetTeachplan != null) {
                    switchTeachplanOrder(targetTeachplan, teachplan);
                }

                break;
            case "movedown":
                // the teachplan cannot move downwards if it is the last one
                int count = getTeachplanCount(teachplan.getCourseId(), teachplan.getParentid());
                if (orderby == count)
                    break;

                queryWrapper.eq(Teachplan::getParentid, teachplan.getParentid());
                queryWrapper.eq(Teachplan::getOrderby, orderby + 1);
                targetTeachplan = teachplanMapper.selectOne(queryWrapper);

                if (targetTeachplan != null) {
                    switchTeachplanOrder(teachplan, targetTeachplan);
                }

                break;
        }
    }

    /**
     * @description 获取最新的排序号
     * @param courseId  课程id
     * @param parentId  父课程计划id
     * @return int 最新排序号
     * @author Mr.M
     * @date 2022/9/9 13:43
     */
    private int getTeachplanCount(Long courseId,Long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teachplan::getCourseId,courseId);
        queryWrapper.eq(Teachplan::getParentid,parentId);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return count;
    }

    public void switchTeachplanOrder(Teachplan teachplanAbove, Teachplan teachplanBelow) {
        Integer aboveOrderby = teachplanAbove.getOrderby();
        Integer belowOrderby = teachplanBelow.getOrderby();

        teachplanAbove.setOrderby(belowOrderby);
        teachplanBelow.setOrderby(aboveOrderby);
        teachplanMapper.updateById(teachplanAbove);
        teachplanMapper.updateById(teachplanBelow);
    }

}
