package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.Building;
import com.campus.learningspace.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/building")
@CrossOrigin
public class BuildingController {

    @Autowired
    private BuildingService buildingService;

    @GetMapping("/list")
    public Result<List<Building>> list() {
        return Result.success(buildingService.getAllActiveBuildings());
    }

    @GetMapping("/{id}")
    public Result<Building> getById(@PathVariable Long id) {
        return Result.success(buildingService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Building building) {
        return Result.success(buildingService.save(building));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Building building) {
        return Result.success(buildingService.updateById(building));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(buildingService.removeById(id));
    }
}
