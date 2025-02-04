from telethon import TelegramClient
import pymysql
import re

# 🔹 Введи свої API-ключі з my.telegram.org
api_id = 27550485  # Заміни на свій API ID
api_hash = "7c0ababc57d36c5dd8bedb4c3a3302af"  # Заміни на свій API HASH
group_username = "ThinkCritical"  # Замінити на @username групи або chat_id

# 🔹 Підключаємося до Telegram через Telethon
client = TelegramClient("session_name", api_id, api_hash)

# 🔹 Підключаємося до MySQL через pymysql
db = pymysql.connect(
    host="localhost",
    user="bestuser",
    password="bestuser",  # Замінити на свій пароль
    database="botdb",
    charset="utf8mb4"
)
cursor = db.cursor()

# 🔹 Регулярний вираз для пошуку посилань на Telegram-канали
telegram_link_pattern = re.compile(r"(t\.me/|https://t\.me/)")

async def fetch_messages():
    async for message in client.iter_messages(group_username, limit=50):
        text = message.text or ""  # Якщо текст None, замінюємо на пустий рядок

        # 🔹 Фільтр: Пропускаємо повідомлення з посиланнями на інші Telegram-канали
        if telegram_link_pattern.search(text):
            print(f"🚫 Пропущено рекламу: {text[:50]}...")
            continue  # Пропускаємо це повідомлення і не додаємо в БД

        image_data = None  # Початково немає фото

        # 🔹 Якщо є фото – скачуємо його
        if message.photo:
            file_path = await message.download_media()
            with open(file_path, "rb") as file:
                image_data = file.read()  # Конвертуємо у BLOB

        # 🔹 Записуємо в базу даних
        sql = "INSERT INTO telegram_posts (chat_id, message_id, text, image) VALUES (%s, %s, %s, %s)"
        cursor.execute(sql, (message.chat_id, message.id, text, image_data))
        db.commit()
        print(f"✅ Додано в базу: {text[:50]}... (Фото: {'Так' if image_data else 'Ні'})")

with client:
    client.loop.run_until_complete(fetch_messages())

db.close()