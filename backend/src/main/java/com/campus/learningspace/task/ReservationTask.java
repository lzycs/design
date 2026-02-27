package com.campus.learningspace.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationTask {

    @Scheduled(cron = "0 0 2 * * ?")
    public void resetWeeklyReservationCount() {
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void releaseExpiredReservations() {
    }
}
