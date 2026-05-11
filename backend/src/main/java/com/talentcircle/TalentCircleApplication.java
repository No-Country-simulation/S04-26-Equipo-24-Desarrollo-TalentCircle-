package com.talentcircle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class TalentCircleApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalentCircleApplication.class, args);
    }

}
