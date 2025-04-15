package telegram.bot.sendtomail;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SenderBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(SenderBot.class);

    private final UserEmailRepository emailRepo;

//    private final String botToken = System.getenv("SENDER_BOT_TOKEN") != null
//            ? System.getenv("SENDER_BOT_TOKEN")
//            : Dotenv.load().get("SENDER_BOT_TOKEN");

    private final String botName = System.getenv("SENDER_BOT_NAME") != null
            ? System.getenv("SENDER_BOT_NAME")
            : Dotenv.load().get("SENDER_BOT_NAME");

    public SenderBot() throws SQLException {
        super(System.getenv("SENDER_BOT_TOKEN") != null
                ? System.getenv("SENDER_BOT_TOKEN")
                : Dotenv.load().get("SENDER_BOT_TOKEN"));
        this.emailRepo = new UserEmailRepository("user_emails.db");
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        String text = message.getText();

        if (text != null) {
            switch (text) {
                case "/start", "🏠 Menu" -> sendMainMenu(chatId);

                case "ℹ️ Help" -> sendTextMessage(chatId,
                        "📌 How to use this bot:\n" +
                                "1. Press 📩 Set Email and enter your address.\n" +
                                "2. Send a photo or file.\n" +
                                "3. It will be emailed automatically!");

                case "📩 Set Email" -> sendTextMessage(chatId,
                        "📬 Please enter your email address:");

                case "📤 Send File" -> sendTextMessage(chatId,
                        "📎 Send me a photo, document, video, or audio file now.");

                default -> {

                    if (text.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
                        try {
                            emailRepo.saveEmail(userId, text);
                            log.info("Email {} for userId: {} added", text, userId);
                            sendTextMessage(chatId, "✅ Email saved: " + text);
                        } catch (Exception e) {
                            sendTextMessage(chatId, "❌ Failed to save email.");
                            log.error("Failed to save email", e);
                            e.printStackTrace();
                        }
                    } else {
                        sendTextMessage(chatId, "❓ Unknown command. Tap 🏠 Menu to start.");
                    }
                }
            }
            return;
        }

        // 1. Handle text input as email setup
        try {
            String receiverEmail = emailRepo.getEmail(userId);
            if (receiverEmail == null) {
                sendTextMessage(message.getChatId(), "❗ Please send your email address first.");
                return;
            }
            java.io.File fileToSend = null;
            String extension = ".dat";
            String subject = "File from Telegram";

            if (message.hasPhoto()) {
                PhotoSize photo = message.getPhoto().stream()
                        .max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElse(null);
                if (photo != null) {
                    extension = ".jpg";
                    fileToSend = downloadTelegramFile(photo.getFileId(), extension);
                    subject = "Photo from Telegram";
                }

            } else if (message.hasDocument()) {
                Document doc = message.getDocument();
                extension = "_" + doc.getFileName(); // keep original name
                fileToSend = downloadTelegramFile(doc.getFileId(), extension);
                subject = "Document from Telegram";

            } else if (message.hasVideo()) {
                Video video = message.getVideo();
                extension = ".mp4";
                fileToSend = downloadTelegramFile(video.getFileId(), extension);
                subject = "Video from Telegram";

            } else if (message.hasAudio()) {
                Audio audio = message.getAudio();
                extension = ".mp3";
                fileToSend = downloadTelegramFile(audio.getFileId(), extension);
                subject = "Audio from Telegram";

            } else if (message.hasVoice()) {
                Voice voice = message.getVoice();
                extension = ".ogg";
                fileToSend = downloadTelegramFile(voice.getFileId(), extension);
                subject = "Voice message from Telegram";
            }

            if (fileToSend != null) {
                EmailSender.sendEmailWithAttachment(receiverEmail, subject, "Telegram file attached", fileToSend);
                sendTextMessage(message.getChatId(), "✅ File sent to " + receiverEmail);
                log.info("File sent to {}", receiverEmail);
            }


        } catch (Exception e) {
            log.error("Failed to send file", e);
            sendTextMessage(message.getChatId(), "❌ Failed to send file: " + e.getMessage());
        }
    }


    private java.io.File downloadTelegramFile(String fileId, String extension) throws Exception {
        org.telegram.telegrambots.meta.api.objects.File telegramFile = execute(new GetFile(fileId));
        String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + telegramFile.getFilePath();

        InputStream in = new URL(fileUrl).openStream();
        java.io.File tempFile = java.io.File.createTempFile("telegram_file_", extension);
        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }


    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMainMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Choose an option:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("📩 Set Email");
        row1.add("📤 Send File");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("ℹ️ Help");

        rows.add(row1);
        rows.add(row2);
        keyboardMarkup.setKeyboard(rows);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
