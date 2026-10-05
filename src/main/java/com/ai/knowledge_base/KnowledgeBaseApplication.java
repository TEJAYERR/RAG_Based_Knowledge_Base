package com.ai.knowledge_base;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@EnableAsync
@SpringBootApplication
public class KnowledgeBaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(KnowledgeBaseApplication.class, args);
	}
}
