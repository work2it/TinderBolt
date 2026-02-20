package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "SergDeev_ai_bot"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = "8467281289:AAGxlHs-HKzRW6N__hZ6DGefrs5RIX-KAU4"; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "javcgkI69gxC3ckPzxOUHNCBh/y1o00JRjNFssihxHBWKdOdr2ibmIufgWV4ld3dZrWBlRbJVJze58CHbsTrtvI7+rVKG3vCOAEKpulzC91bOlKxude9NK/N2V764mqKnhbguJ1U0EV1Wnpbit4qDoqKCfmHBB7tNmpRuM+d37TBvYwmwM8l2+jIiwPqaYxviAt2tL8UggF9VxuubtldnUGkR9/ckEPhSzHgRzaPD9fNhVhLE="; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);

    private DialogMode currentMode = null;

    private ArrayList<String> list = new ArrayList<>();

    private UserInfo me;

    private int questionCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();

        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");
            sendTextMessage(text);
            showMainMenu("главное меню", "/start",
                        "генерация Tinder-профля", "/profile",
                        "сообщение для знакомства", "/opener",
                        "переписка от вашего имени", "/message",
                        "переписка со звездами", "/date",
                        "задать вопрос чату GPT", "/gpt"
                    );
            return;
        }

        if (message.equals("/gpt")) {
            currentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            sendTextMessage("Введите сообщение для *ChatGPT:*");
            return;
        }

        if (message.equals("/profile")) {
            currentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");
            sendTextMessage("Скольво тебе лет?");
            me = new UserInfo();
            questionCount = 0;
            return;
        }

        if (message.equals("/opener")) {
            currentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");
            sendTextMessage("Кого ищите?");
            me = new UserInfo();
            questionCount = 0;
            return;
        }

        if (message.equals("/message")) {
            currentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите в чат вашу переписку",
                    "Следующее сообщение","message_next",
                    "Пригласить на свидание","message_date");
            return;
        }

        if (message.equals("/date")) {
            currentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райн Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }

        if (currentMode == DialogMode.PROFILE) {
            if (questionCount == 0) {
                me.age = message;
                questionCount = 1;
                sendTextMessage("Какой пол?");
                return;
            }
            if (questionCount == 1) {
                me.sex = message;
                questionCount = 2;
                sendTextMessage("Ваше хобби?");
                return;
            }
            if (questionCount == 2) {
                me.hobby = message;
                String prompt = loadPrompt("profile");
                Message msg = sendTextMessage("Отправка данных в ChatGPT..");
                String answer = chatGPT.sendMessage(prompt, me.toString());
                updateTextMessage(msg, answer);
                return;
            }
        }

        if (currentMode == DialogMode.OPENER) {
            if (questionCount == 0) {
                me.sex = message;
                questionCount = 1;
                sendTextMessage("Какого возраста?");
                return;
            }
            if (questionCount == 1) {
                me.age = message;
                questionCount = 2;
                sendTextMessage("Цель знакомства?");
                return;
            }
            if (questionCount == 2) {
                me.goals = message;
                String prompt = loadPrompt("opener");
                Message msg = sendTextMessage("Отправка данных в ChatGPT..");
                String answer = chatGPT.sendMessage(prompt, me.toString());
                updateTextMessage(msg, answer);
                return;
            }
        }

        if (currentMode == DialogMode.GPT) {
            String prompt = loadPrompt("gpt");
            String answer = chatGPT.sendMessage(prompt, message);
            sendTextMessage(answer);
            return;
        }

        if (currentMode == DialogMode.MESSAGE) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);
                Message msg = sendTextMessage("Отправка данных в ChatGPT..");
                String answer = chatGPT.sendMessage(prompt, userChatHistory);
                updateTextMessage(msg, answer);
            }
            list.add(message);
            return;
        }

        if (currentMode == DialogMode.DATE) {
            String query = getCallbackQueryButtonKey();

            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage("Попробуйте пригласить на свидание за 5 сообщений");
                String prompt =loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }

            String answer = chatGPT.addMessage(message);
            sendTextMessage(answer);
            return;
        }
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
