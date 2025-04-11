package telegram.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Comparator;


public class SenderBot extends TelegramLongPollingBot {

    private final UserEmailRepository emailRepo;

    String botToken = System.getenv("BOT_TOKEN");
    String botName = System.getenv("BOT_NAME");

    public SenderBot(UserEmailRepository emailRepo) {
        this.emailRepo = emailRepo;
    }


    @Override
    public String getBotToken() {
        return botToken;
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
        // 1. Handle text input as email setup
        if (message.hasText()) {
            String input = message.getText().trim();
            if (input.matches("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$")) {
                try {
                    emailRepo.saveEmail(userId, input);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                sendTextMessage(message.getChatId(), "✅ Email set to: " + input);
            } else {
                sendTextMessage(message.getChatId(), "📬 Please send your email address to receive files.");
            }
            return;
        }
        try {
            String receiverEmail = emailRepo.getEmail(userId);
            if (receiverEmail == null) {
                sendTextMessage(message.getChatId(), "❗ Please send your email address first.");
                return;
            }
            java.io.File fileToSend = null;
            String extension = ".dat"; // default fallback
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
            }


        } catch (Exception e) {
            e.printStackTrace();
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
}
