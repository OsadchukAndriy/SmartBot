package com.bot.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.bot.model.Post;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TelegramPublisher {

    private static final String URL_DB = "jdbc:mysql://localhost:3306/botdb?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true";
    private static final String USER = "bestuser";
    private static final String PASSWORD = "bestuser";

    private static final String BOT_TOKEN = "7533612978:AAHBUoCQKzTX9DaP4ArQfFTkd5qtqeeFH0w"; // Встав реальний токен
    private static final String CHAT_ID = "-1002330231296"; // Встав реальний Chat ID

    @Scheduled(fixedRate = 60000) // 10 хвилин (600,000 мс)
    public void publishNewPosts() {
        System.out.println("🔍 Перевіряємо наявність нових постів для публікації...");

        Post post = fetchUnpublishedPost();
        if (post != null) {
            System.out.println("📢 Готуємо до публікації пост: " + post.getText());

            boolean isPublished = sendToTelegram(post.getText(), post.getImage());
            if (isPublished) {
                markPostAsPublished(post.getId());
                System.out.println("✅ Пост успішно опубліковано!");
            } else {
                System.out.println("❌ Помилка під час публікації поста.");
            }
        } else {
            System.out.println("❌ Немає нових постів для публікації.");
        }
    }

    private Post fetchUnpublishedPost() {
        String sql = "SELECT id, new_text, new_image FROM original_posts WHERE published = FALSE ORDER BY id ASC LIMIT 1";

        try (Connection connection = DriverManager.getConnection(URL_DB, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                int id = resultSet.getInt("id");
                String text = resultSet.getString("new_text");
                byte[] image = resultSet.getBytes("new_image");
                return new Post(id, text, image);
            }
        } catch (SQLException e) {
            System.err.println("❌ Помилка при отриманні неопублікованих постів: " + e.getMessage());
        }
        return null;
    }

    private void markPostAsPublished(int postId) {
        String sql = "UPDATE original_posts SET published = TRUE WHERE id = ?";

        try (Connection connection = DriverManager.getConnection(URL_DB, USER, PASSWORD);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, postId);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Помилка при оновленні статусу поста: " + e.getMessage());
        }
    }

    private boolean sendToTelegram(String text, byte[] image) {
        try {
            text = cleanMarkdownText(text);

            String apiUrl;
            if (image == null) {
                apiUrl = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";
                JSONObject json = new JSONObject();
                json.put("chat_id", CHAT_ID);
                json.put("text", text);
                json.put("parse_mode", "Markdown");

                return sendPostRequest(apiUrl, json.toString());
            } else {
                byte[] processedImage = processImage(image);
                apiUrl = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendPhoto";
                return sendPostRequestWithImage(apiUrl, text, processedImage);
            }
        } catch (Exception e) {
            System.err.println("❌ Помилка під час відправки повідомлення: " + e.getMessage());
            return false;
        }
    }

    private byte[] processImage(byte[] image) {
        try {
            InputStream is = new ByteArrayInputStream(image);
            BufferedImage bufferedImage = ImageIO.read(is);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpeg", os);
            return os.toByteArray();
        } catch (Exception e) {
            System.err.println("❌ Помилка при обробці зображення: " + e.getMessage());
            return image;
        }
    }

    private String cleanMarkdownText(String text) {
        if (text == null) return "";

        return text.replace("*", "\\*")
                .replace("_", "\\_")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("`", "\\`");
    }

    private boolean sendPostRequest(String apiUrl, String payload) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream();
                 OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8")) { // Явно вказуємо UTF-8
                writer.write(payload);
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("🔹 Відповідь Telegram API: " + responseCode);

            if (responseCode != 200) {
                BufferedReader errorStream = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String errorMessage = errorStream.readLine();
                System.err.println("❌ Помилка від Telegram API: " + errorMessage);
            }

            return responseCode == 200;
        } catch (Exception e) {
            System.err.println("❌ Помилка при виконанні POST-запиту: " + e.getMessage());
            return false;
        }
    }

    private boolean sendPostRequestWithImage(String apiUrl, String caption, byte[] image) {
        try {
            String boundary = "----Boundary";
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept-Charset", "UTF-8");

            if (caption.length() > 1024) {
                caption = caption.substring(0, 1021) + "...";
            }

            try (OutputStream os = connection.getOutputStream();
                 DataOutputStream dos = new DataOutputStream(os)) {

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
                dos.writeBytes(CHAT_ID + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"caption\"\r\n\r\n");
                dos.writeBytes(caption + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"image.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.write(image);
                dos.writeBytes("\r\n--" + boundary + "--\r\n");
                dos.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("🔹 Відповідь Telegram API: " + responseCode);

            if (responseCode != 200) {
                BufferedReader errorStream = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String errorMessage = errorStream.readLine();
                System.err.println("❌ Telegram API Error: " + errorMessage);
            }

            return responseCode == 200;
        } catch (Exception e) {
            System.err.println("❌ Помилка при відправці фото: " + e.getMessage());
            return false;
        }
    }
}