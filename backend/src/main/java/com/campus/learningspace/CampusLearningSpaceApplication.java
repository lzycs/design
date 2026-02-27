package com.campus.learningspace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampusLearningSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusLearningSpaceApplication.class, args);
    }

}
