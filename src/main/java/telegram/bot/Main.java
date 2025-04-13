package telegram.bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import telegram.bot.sendtomail.SenderBot;
import telegram.bot.sendtomail.UserEmailRepository;
import telegram.bot.translator.DeeplTranslator;
import telegram.bot.translator.TranslateBot;
import telegram.bot.translator.UserPreferenceRepository;

public class Main {
    public static void main(String[] args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        final UserPreferenceRepository preferenceRepo;
        final DeeplTranslator translator;
        final UserEmailRepository emailRepo = new UserEmailRepository("user_emails.db");

        botsApi.registerBot(new SenderBot(emailRepo));
        botsApi.registerBot(new TranslateBot());
    }
}