package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/2 16:12
 */
@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Autowired
    MyCourseTablesService myCourseTablesService;
//
//    @Autowired
//    MyCourseTablesServiceImpl currentProxy;

    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        //课程收费标准
        String charge = coursepublish.getCharge();
        //选课记录
        XcChooseCourse chooseCourse = null;
        if ("201000".equals(charge)) {//课程免费
            //添加免费课程
            chooseCourse = addFreeCourse(userId, coursepublish);
            //添加到我的课程表
            addCourseTabls(chooseCourse);
        } else {
            //添加收费课程
            chooseCourse = addChargeCoruse(userId, coursepublish);
        }
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(chooseCourse, xcChooseCourseDto);
        //获取学习资格
        XcCourseTablesDto xcCourseTablesDto = getLearningStatus(userId, courseId);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());

        return xcChooseCourseDto;
    }

    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        // search the course among all the courses owned by the user
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);

        // if the course is not found, return the status of not having chosen the course or
        // not having paid after choosing the course (still in the cart)
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        if (xcCourseTables == null) {
            xcCourseTablesDto.setLearnStatus("702002"); // not chosen or not paid after choosing
            return xcCourseTablesDto;
        }

        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if (!isExpires) xcCourseTablesDto.setLearnStatus("702001"); // normal learning
        else xcCourseTablesDto.setLearnStatus("702003"); // expired, need to apply for renewal or pay again

        BeanUtils.copyProperties(xcCourseTables, xcCourseTablesDto);

        return xcCourseTablesDto;
    }

    @Override
    public PageResult<XcCourseTables> mycoursetables(MyCourseTableParams params) {
        int page = params.getPage();
        int size = params.getSize();

        Page<XcCourseTables> xcCourseTablesPage = new Page<>(page, size);
        LambdaQueryWrapper<XcCourseTables> queryWrapper =
                new LambdaQueryWrapper<XcCourseTables>()
                        .eq(XcCourseTables::getUserId, params.getUserId());

        Page<XcCourseTables> result = xcCourseTablesMapper.selectPage(xcCourseTablesPage, queryWrapper);
        List<XcCourseTables> records = result.getRecords();
        long total = result.getTotal();

        return new PageResult<>(records, total, page, size);
    }

    // add the free course into 1) the course selection record table and then into 2) the user's course table
    public XcChooseCourse addFreeCourse(String userId, CoursePublish coursepublish) {
        // see if the free course is already present in the course selection record table
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001")//免费课程
                .eq(XcChooseCourse::getStatus, "701001");//选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && !xcChooseCourses.isEmpty()) {
            return xcChooseCourses.get(0);
        }

        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        xcChooseCourseMapper.insert(xcChooseCourse);

        return xcChooseCourse;
    }

    //添加收费课程
    public XcChooseCourse addChargeCoruse(String userId, CoursePublish coursepublish) {
        //查询选课记录表是否存在免费的且选课成功的订单
        LambdaQueryWrapper<XcChooseCourse> queryWrapper =
                new LambdaQueryWrapper<XcChooseCourse>()
                        .eq(XcChooseCourse::getUserId, userId)
                        .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                        .eq(XcChooseCourse::getOrderType, "700002")// charged course
                        .eq(XcChooseCourse::getStatus, "701002");// not yet paid

        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && !xcChooseCourses.isEmpty()) return xcChooseCourses.get(0);

        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");// paid course
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setStatus("701002");// wait for payment

        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));
        xcChooseCourseMapper.insert(xcChooseCourse);

        return xcChooseCourse;
    }

    //添加到我的课程表
    public XcCourseTables addCourseTabls(XcChooseCourse xcChooseCourse) {
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)) XueChengPlusException.cast("选课未成功，无法添加到课程表");

        // see if the course is already present in the user's course table
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if (xcCourseTables != null) return xcCourseTables;

        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse, xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTables.setUpdateDate(LocalDateTime.now());

        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if (insert <= 0) XueChengPlusException.cast("添加课程表失败");

        return xcCourseTables;
    }

    /**
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @description 根据课程和用户查询我的课程表中某一门课程
     * @author Mr.M
     * @date 2022/10/2 17:07
     */
    public XcCourseTables getXcCourseTables(String userId, Long courseId) {
        return xcCourseTablesMapper.selectOne(
                new LambdaQueryWrapper<XcCourseTables>()
                        .eq(XcCourseTables::getUserId, userId)
                        .eq(XcCourseTables::getCourseId, courseId));

    }


}

