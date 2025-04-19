package com.zakpruitt.scalpatron9000;

import com.zakpruitt.scalpatron9000.util.LoginAndSaveStateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class Scalpatron9000Application implements CommandLineRunner {

	@Value("${target.login-only}")
	private boolean loginOnly;

	public static void main(String[] args) {
		SpringApplication.run(Scalpatron9000Application.class, args);
	}

	@Override
	public void run(String... args) {
		if (loginOnly) {
			log.info("Login only switch turned one!");
			LoginAndSaveStateUtil.run("my-target-profile", "src/main/resources/target-storage.json");
			System.exit(0);
		}
	}

}
