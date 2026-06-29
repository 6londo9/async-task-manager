package com.backendDojo.asyncTaskManager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;

@EnableResilientMethods
@SpringBootApplication
public class AsyncTaskManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsyncTaskManagerApplication.class, args);
    }
}
