package com.ats.bajaj.finserv;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BajajFinservApplication {

	public static void main(String[] args) {
		SpringApplication.run(BajajFinservApplication.class, args);
	}

	@Bean
	public CommandLineRunner runner(BajajFinserv service) {
		return args -> service.work();
	}

}
