import com.apex.bot.TelegramMessageHandler;
import com.apex.bot.TelegramSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpamBot {

    private static final String BOT_TOKEN = "664087722:AAFWm9kCQ0pIthEo3Kprq3P1EHTnT1BP5rQ";
    private static final String BOT_NAME = "ApexSpamBot";
    private static final Logger log = LoggerFactory.getLogger(SpamBot.class);

    public static void main(String[] args){
        TelegramSessionManager telegramSessionManager = new TelegramSessionManager();

        Thread open = new Thread(() -> {
            try {
                telegramSessionManager.addPollingBot(new TelegramMessageHandler(BOT_TOKEN, BOT_NAME));
                telegramSessionManager.start();
                log.info("Bot started");
                while (true);
            } catch (Exception e) {
                log.error("Something went wrong: "+ e.getCause().getMessage());
            } finally {
                telegramSessionManager.stop();
                log.info("Bot down");
            }
        });
        open.start();
    }
}
