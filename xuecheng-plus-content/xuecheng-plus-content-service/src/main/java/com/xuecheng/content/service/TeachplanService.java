package com.xuecheng.content.service;


import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {
    public List<TeachplanDto> findTeachplanTree(Long courseId);

    public void saveTeachplan(SaveTeachplanDto teachplan);

    public void deleteTeachplan(Long teachplanId);

    public void moveTeachplan(String movement, Long teachplanId);

    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto);
}
