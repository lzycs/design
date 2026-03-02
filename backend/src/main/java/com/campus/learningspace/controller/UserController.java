package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.User;
import com.campus.learningspace.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return Result.error(400, "用户名和密码不能为空");
        }
        User existing = userService.findByUsername(user.getUsername());
        if (existing != null) {
            return Result.error(400, "用户名已存在");
        }
        user.setId(null);
        userService.save(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody User loginReq) {
        if (loginReq.getUsername() == null || loginReq.getPassword() == null) {
            return Result.error(400, "用户名和密码不能为空");
        }
        User user = userService.findByUsername(loginReq.getUsername());
        if (user == null || !loginReq.getPassword().equals(user.getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error(403, "账号已被禁用");
        }
        user.setPassword(null);
        return Result.success(user);
    }
}

