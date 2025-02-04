package com.bot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final String BOT_USERNAME = "parsingAllGroup_bot";
    private static final String BOT_TOKEN = "7533612978:AAHBUoCQKzTX9DaP4ArQfFTkd5qtqeeFH0w";

    public TelegramBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equalsIgnoreCase("/start")) {
                sendTextMessage(chatId, "Привіт! Я AI-бот для парсингу контенту. Напиши /help, щоб дізнатися більше.");
            } else if (messageText.equalsIgnoreCase("/help")) {
                sendTextMessage(chatId, "Ось список доступних команд:\n" +
                        "/start - Запуск бота\n" +
                        "/help - Допомога\n" +
                        "🚀 Скоро будуть нові функції!");
            } else {
                sendTextMessage(chatId, "Я не розумію цю команду. Використай /help для списку команд.");
            }
        }
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}