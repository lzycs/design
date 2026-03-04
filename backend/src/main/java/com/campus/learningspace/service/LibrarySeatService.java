package com.campus.learningspace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learningspace.entity.LibrarySeat;
import com.campus.learningspace.mapper.LibrarySeatMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LibrarySeatService extends ServiceImpl<LibrarySeatMapper, LibrarySeat> {

    public List<LibrarySeat> getSeatsByLibraryAndFloor(Long libraryId, Integer floor) {
        LambdaQueryWrapper<LibrarySeat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LibrarySeat::getLibraryId, libraryId)
                .eq(LibrarySeat::getFloor, floor)
                .eq(LibrarySeat::getStatus, 1);
        return list(wrapper);
    }
}

