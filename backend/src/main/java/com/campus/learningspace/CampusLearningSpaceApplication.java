package com.campus.learningspace;

import com.campus.learningspace.config.ReservationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ReservationProperties.class)
public class CampusLearningSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusLearningSpaceApplication.class, args);
    }

}
