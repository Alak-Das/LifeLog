package com.lifelog.ehr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LifeLogApplication {

	public static void main(String[] args) {
		SpringApplication.run(LifeLogApplication.class, args);
	}

}
