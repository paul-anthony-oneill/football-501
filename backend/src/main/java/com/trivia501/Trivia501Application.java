package com.trivia501;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Trivia501Application {

    public static void main(String[] args) {
        SpringApplication.run(Trivia501Application.class, args);
    }
}
