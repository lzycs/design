package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.Library;
import com.campus.learningspace.mapper.LibraryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibraryService extends ServiceImpl<LibraryMapper, Library> {

    public List<Library> getActiveLibraries() {
        LambdaQueryWrapper<Library> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Library::getStatus, 1);
        return list(wrapper);
    }
}

