package com.pecuni;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // required later for recurring-transaction generation (week 5 of the work plan)
public class PecuniApplication {

    public static void main(String[] args) {
        SpringApplication.run(PecuniApplication.class, args);
    }
}
