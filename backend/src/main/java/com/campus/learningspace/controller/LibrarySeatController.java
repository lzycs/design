package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.LibrarySeat;
import com.campus.learningspace.service.LibrarySeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library-seat")
@CrossOrigin
public class LibrarySeatController {

    @Autowired
    private LibrarySeatService librarySeatService;

    @GetMapping("/library/{libraryId}/floor/{floor}")
    public Result<List<LibrarySeat>> getSeatsByLibraryAndFloor(@PathVariable Long libraryId,
                                                               @PathVariable Integer floor) {
        return Result.success(librarySeatService.getSeatsByLibraryAndFloor(libraryId, floor));
    }
}

