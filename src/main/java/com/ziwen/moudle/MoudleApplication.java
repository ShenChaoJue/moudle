package com.ziwen.moudle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoudleApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoudleApplication.class, args);
	}

}
