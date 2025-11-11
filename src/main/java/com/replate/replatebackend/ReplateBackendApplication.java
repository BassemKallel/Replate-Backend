package com.replate.replatebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ReplateBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReplateBackendApplication.class, args);
    }

}
