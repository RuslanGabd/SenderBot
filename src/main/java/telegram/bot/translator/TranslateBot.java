package telegram.bot.translator;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TranslateBot extends TelegramLongPollingBot {

    private final Map<Long, String> userTargetLangMap = new HashMap<>();
    private final Map<Long, String> userSourceLangMap = new HashMap<>();
    private final Map<Long, String> userLanguageMode = new HashMap<>();

    DeeplTranslator translator = new DeeplTranslator();

    private final String botToken = System.getenv("TRANSLATE_BOT_TOKEN") != null
            ? System.getenv("TRANSLATE_BOT_TOKEN")
            : Dotenv.load().get("TRANSLATE_BOT_TOKEN");

    private final String botName = System.getenv("TRANSLATE_BOT_NAME") != null
            ? System.getenv("TRANSLATE_BOT_NAME")
            : Dotenv.load().get("TRANSLATE_BOT_NAME");

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
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
                case "/start", "🏠 Main Menu" -> {
                    sendTextMessage(chatId, "Welcome to TranslateBot!");
                    sendLanguageMenu(chatId);
                    return;
                }

                case "🌐 Set Source Language" -> {
                    userLanguageMode.put(userId, "source");
                    sendTextMessage(chatId, "📥 Select your source language:");
                    return;
                }

                case "🌐 Set Target Language" -> {
                    userLanguageMode.put(userId, "target");
                    sendTextMessage(chatId, "📤 Select your target language:");
                    return;
                }
            }

            // Handle actual language choice
            if (List.of("EN", "RU", "FR", "DE", "ES").contains(text.replaceAll("[^A-Z]", ""))) {
                String langCode = text.replaceAll("[^A-Z]", "");
                String mode = userLanguageMode.getOrDefault(userId, "target");

                if (mode.equals("source")) {
                    userSourceLangMap.put(userId, langCode);
                    sendTextMessage(chatId, "📥 Source language set to " + langCode);
                } else {
                    userTargetLangMap.put(userId, langCode);
                    sendTextMessage(chatId, "📤 Target language set to " + langCode);
                }
                return;
            }

            // Handle translation
            if (userTargetLangMap.containsKey(userId) && userSourceLangMap.containsKey(userId)) {
                String sourceLang = userSourceLangMap.get(userId);
                String targetLang = userTargetLangMap.get(userId);
                String translated = translator.translate(text, sourceLang, targetLang);

                sendTextMessage(chatId, "🔁 " + text + "\n✅ " + translated);
            } else {
                sendTextMessage(chatId, "❗ Please set both source and target languages first.");
                sendLanguageMenu(chatId);
            }
        }
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

    private void sendLanguageMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🌐 Choose target language:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🌐 Set Source Language");
        row1.add("🌐 Set Target Language");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🇷🇺 RU");
        row2.add("🇬🇧 EN");
        row2.add("🇫🇷 FR");
        row2.add("🇩🇪 DE");
        row2.add("🇪🇸 ES");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🏠 Main Menu");

        keyboard.setKeyboard(List.of(row1, row2, row3));
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
