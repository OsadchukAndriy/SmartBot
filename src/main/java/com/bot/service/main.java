package com.bot.service;

import org.springframework.beans.factory.annotation.Value;

public class main {
    @Value("${openai.api.key}")  // Читаємо значення з application.properties
    private static String openAiApiKey;

    static String apiKey = System.getenv("OPENAI_API_KEY");

        public static void main(String[] args) {

            System.out.println("🔑 OpenAI API Key: " + openAiApiKey);

            System.out.println("API Key: " + apiKey);
        }
    }

