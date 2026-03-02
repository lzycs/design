package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.Library;
import com.campus.learningspace.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@CrossOrigin
public class LibraryController {

    @Autowired
    private LibraryService libraryService;

    @GetMapping("/list")
    public Result<List<Library>> list() {
        return Result.success(libraryService.getActiveLibraries());
    }
}

