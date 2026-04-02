package com.workflow.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WorkflowEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkflowEngineApplication.class, args);
    }
}