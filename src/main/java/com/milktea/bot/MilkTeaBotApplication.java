package com.milktea.bot;

import com.milktea.bot.telegram.MilkTeaBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class MilkTeaBotApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MilkTeaBotApplication.class);

    private final MilkTeaBot bot;

    public MilkTeaBotApplication(MilkTeaBot bot) {
        this.bot = bot;
    }

    public static void main(String[] args) {
        SpringApplication.run(MilkTeaBotApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String token = bot.getBotToken();
        if (token == null || token.isBlank() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            log.error("BOT_TOKEN chưa được cấu hình!");
            log.error("Vui lòng set biến môi trường BOT_TOKEN:");
            log.error("  PowerShell: $env:BOT_TOKEN=\"your_token\"");
            log.error("  Hoặc sửa file application.yml");
            return;
        }

        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
            log.info("Milk Tea Bot started successfully!");
        } catch (TelegramApiException e) {
            log.error("Không thể khởi động bot. Kiểm tra lại BOT_TOKEN có đúng không!", e);
        }
    }
}
