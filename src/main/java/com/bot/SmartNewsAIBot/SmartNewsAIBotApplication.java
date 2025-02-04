package com.bot.SmartNewsAIBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@ComponentScan(basePackages = "com.bot")
@SpringBootApplication
@EnableScheduling
public class SmartNewsAIBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartNewsAIBotApplication.class, args);  // 🔹 Запускаємо telegram_parser.py
		startTelegramParser();
	}

	private static void startTelegramParser() {
		try {
			System.out.println("🚀 Запускаємо Telegram Parser...");
			ProcessBuilder processBuilder = new ProcessBuilder("python3", "telegram_parser.py");
			processBuilder.inheritIO(); // Дозволяє побачити вивід Python у консолі
			processBuilder.start();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("❌ Помилка при запуску telegram_parser.py");
		}
	}

}
