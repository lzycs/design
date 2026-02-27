package com.campus.learningspace.controller;

import com.campus.learningspace.common.Result;
import com.campus.learningspace.entity.Reservation;
import com.campus.learningspace.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservation")
@CrossOrigin
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping("/user/{userId}")
    public Result<List<Reservation>> getUserReservations(@PathVariable Long userId) {
        return Result.success(reservationService.getUserReservations(userId));
    }

    @GetMapping("/classroom/{classroomId}")
    public Result<List<Reservation>> getClassroomReservations(
            @PathVariable Long classroomId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.success(reservationService.getClassroomReservations(classroomId, date));
    }

    @GetMapping("/{id}")
    public Result<Reservation> getById(@PathVariable Long id) {
        return Result.success(reservationService.getById(id));
    }

    @PostMapping
    public Result<Boolean> save(@RequestBody Reservation reservation) {
        return Result.success(reservationService.save(reservation));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody Reservation reservation) {
        return Result.success(reservationService.updateById(reservation));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(reservationService.removeById(id));
    }
}
