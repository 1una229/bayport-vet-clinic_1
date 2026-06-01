package com.bayport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BayportApplication {

    public static void main(String[] args) {
        SpringApplication.run(BayportApplication.class, args);
    }
}
