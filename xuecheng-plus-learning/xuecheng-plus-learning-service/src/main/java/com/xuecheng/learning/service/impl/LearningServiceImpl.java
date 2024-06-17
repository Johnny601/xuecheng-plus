package com.xuecheng.learning.service.impl;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LearningServiceImpl implements LearningService {
    @Autowired
    MyCourseTablesService myCourseTablesService;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Autowired
    MediaServiceClient mediaServiceClient;

    @Override
    public RestResponse<String> getVideo(String userId, Long courseId, Long teachplanId, String mediaId) {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if(coursepublish==null) XueChengPlusException.cast("课程信息不存在");


        if (StringUtils.isNotEmpty(userId)) {
            XcCourseTablesDto xcCourseTablesDto = myCourseTablesService.getLearningStatus(userId, courseId);
            String learnStatus = xcCourseTablesDto.getLearnStatus();

            if ("702002".equals(learnStatus) || "702003".equals(learnStatus)) {
                return RestResponse.validfail("您的选课已过期需要申请续期或重新支付");
            } else {
                return mediaServiceClient.getPlayUrlByMediaId(mediaId);
            }
        }

        // if the user does not log in
        String charge = coursepublish.getCharge();
        if ("201000".equals(charge)) {
            return mediaServiceClient.getPlayUrlByMediaId(mediaId);
        }

        return RestResponse.validfail("您没有学习资格");
    }
}
