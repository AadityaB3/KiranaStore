package com.kirana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SmartKiranaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartKiranaApplication.class, args);
    }
}
