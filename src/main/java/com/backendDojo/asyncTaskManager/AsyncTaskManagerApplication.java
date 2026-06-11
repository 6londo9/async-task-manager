package com.backendDojo.asyncTaskManager;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AsyncTaskManagerApplication implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;

    public AsyncTaskManagerApplication(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public static void main(String[] args) {
        SpringApplication.run(AsyncTaskManagerApplication.class, args);
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanDefinitionName : applicationContext.getBeanDefinitionNames()) {
            if (beanDefinitionName.contains("HikariMetrics")) {
                System.out.println(beanDefinitionName);
            }
        }

    }
}
