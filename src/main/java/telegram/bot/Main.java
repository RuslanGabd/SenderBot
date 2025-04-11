package telegram.bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) throws Exception {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
         final UserEmailRepository emailRepo = new UserEmailRepository("user_emails.db");
        botsApi.registerBot(new SenderBot(emailRepo));
    }
}