package com.ibetcha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IbetchaApplication {

    public static void main(String[] args) {
        SpringApplication.run(IbetchaApplication.class, args);
    }
}
