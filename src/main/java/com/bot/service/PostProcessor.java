package com.bot.service;

import com.bot.model.Post;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;

@Component
public class PostProcessor {
    private static final String URL = "jdbc:mysql://localhost:3306/botdb?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true";
    private static final String USER = "bestuser";
    private static final String PASSWORD = "bestuser";

    static String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    // 🔹 Виконується кожну хвилину
    @Scheduled(fixedRate = 6000)
    public void processNewPosts() {
        System.out.println("🔍 Перевіряємо нові пости...");
        Post post = fetchUnprocessedPost();
        if (post != null) {
            System.out.println("📩 Вибраний пост: " + post.getText());

            // 🔹 Генеруємо новий текст через GPT-4
            String newText = rewriteText(post.getText());
            System.out.println("✍️ Оновлений текст: " + newText);

            // 🔹 Генеруємо нове зображення (поки що без змін)
            byte[] newImage = modifyImage(post.getImage());
            System.out.println("🖼️ Зображення змінене!");

            // 🔹 Зберігаємо в `original_posts`
            saveOriginalPost(post.getId(), newText, newImage);

            // 🔹 Позначаємо пост як оброблений
            markPostAsProcessed(post.getId());

            // 🔹 Переконуємось, що в Telegram піде правильний текст
            if (!newText.startsWith("❌") && newText.length() > 10) {
                System.out.println("📢 Надсилаємо в Telegram: " + newText);
            } else {
                System.out.println("⚠️ Виявлено помилку, не публікуємо в Telegram!");
            }

            System.out.println("✅ Пост позначено як оброблений!");
        } else {
            System.out.println("❌ Немає нових постів для обробки.");
        }
    }

    // 🔹 Отримання НЕОБРОБЛЕНОГО поста
    private Post fetchUnprocessedPost() {
        String sql = "SELECT id, text, image FROM telegram_posts WHERE processed = FALSE ORDER BY id ASC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int id = rs.getInt("id");
                String text = rs.getString("text");
                byte[] image = rs.getBytes("image");
                return new Post(id, text, image);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 🔹 Позначаємо пост як оброблений
    private void markPostAsProcessed(int postId) {
        String sql = "UPDATE telegram_posts SET processed = TRUE WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, postId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 🔹 Використання GPT-4 для зміни тексту
    private String rewriteText(String originalText) {
        System.out.println("🔑 OpenAI API Key: " + OPENAI_API_KEY);

        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isEmpty()) {
            System.out.println("❌ Помилка: API-ключ OpenAI не знайдено.");
            return "❌ Помилка: API-ключ не налаштований.";
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(OPENAI_API_URL);
            request.setHeader("Authorization", "Bearer " + OPENAI_API_KEY);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept-Charset", "UTF-8");

            // Формуємо JSON-запит
            JSONObject json = new JSONObject();
            json.put("model", "gpt-3.5-turbo");
            json.put("temperature", 0.7);
            json.put("max_tokens", 500);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content",
                    "Ти — AI, який повинен кардинально перефразувати текст, роблячи його більш унікальним, цікавим і легким для читання. " +
                            "Зміни структуру речень, додай синоніми та спрость складні фрази. " +
                            "Збережи сенс, але текст має бути повністю унікальним."
            ));
            messages.put(new JSONObject().put("role", "user").put("content", "Перефразуй цей текст: " + originalText));

            json.put("messages", messages);
            System.out.println("📩 JSON-запит до OpenAI: " + json.toString(2));

            StringEntity entity = new StringEntity(json.toString(), "UTF-8");
            request.setEntity(entity);

            // Відправляємо запит до GPT-4
            try (CloseableHttpResponse response = client.execute(request);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                System.out.println("📝 OpenAI API Response: " + result.toString());

                JSONObject responseJson = new JSONObject(result.toString());

                // ✅ Перевірка на помилку у відповіді API
                if (responseJson.has("error")) {
                    String errorMessage = responseJson.getJSONObject("error").getString("message");
                    System.out.println("❌ OpenAI API Error: " + errorMessage);
                    return "❌ Помилка OpenAI: " + errorMessage;
                }

                return responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Помилка при перефразуванні тексту.";
        }
    }

    // 🔹 Заглушка для зміни зображень
    private byte[] modifyImage(byte[] imageData) {
        return imageData;
    }

    // 🔹 Збереження зміненого поста
    private void saveOriginalPost(int originalId, String newText, byte[] newImage) {
        String sql = "INSERT INTO original_posts (original_post_id, new_text, new_image) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, originalId);
            stmt.setString(2, newText);
            stmt.setBytes(3, newImage);
            stmt.executeUpdate();
            System.out.println("✅ Оновлений пост збережено в `original_posts`!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}