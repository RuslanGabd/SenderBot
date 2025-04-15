package telegram.bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.bot.sendtomail.SenderBot;
import telegram.bot.translator.DeeplTranslator;
import telegram.bot.translator.TranslateBot;
import telegram.bot.translator.UserPreferenceRepository;

public class Main {
    public static void main(String[] args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        final UserPreferenceRepository preferenceRepo;
        final DeeplTranslator translator;

        botsApi.registerBot(new SenderBot());
        botsApi.registerBot(new TranslateBot());
    }
}