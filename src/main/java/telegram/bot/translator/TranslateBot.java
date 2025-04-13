package telegram.bot.translator;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TranslateBot extends TelegramLongPollingBot {

    private final Map<Long, String> userLangMap = new HashMap<>();


    DeeplTranslator translator = new DeeplTranslator();
    String botToken = System.getenv("BOT_TOKEN");
    String botName = System.getenv("BOT_NAME");
    @Override
    public String getBotUsername() {
        return botName; // your bot username
    }

    @Override
    public String getBotToken() {
        return botToken; // in Railway env vars
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        String text = message.getText();
        if (text == null) return;

        switch (text) {
            case "/start", "🏠 Main Menu" -> {
                sendTextMessage(chatId, "Welcome to TranslateBot! Choose a target language.");
                sendLanguageMenu(chatId);
                return;
            }

            case "🇷🇺 Russian" -> userLangMap.put(userId, "RU");
            case "🇫🇷 French"  -> userLangMap.put(userId, "FR");
            case "🇪🇸 Spanish" -> userLangMap.put(userId, "ES");
            case "🇩🇪 German"  -> userLangMap.put(userId, "DE");
        }

        // If user has selected a target language and sends a word
        if (userLangMap.containsKey(userId)) {
            String targetLang = userLangMap.get(userId);
            String translated = translator.translate(text, "EN", targetLang);
            sendTextMessage(chatId, "🔁 " + text + "\n✅ " + translated);
        } else {
            sendTextMessage(chatId, "❗ Please choose a target language first.");
            sendLanguageMenu(chatId);
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
        row1.add("🇷🇺 Russian");
        row1.add("🇫🇷 French");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🇪🇸 Spanish");
        row2.add("🇩🇪 German");

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
