package org.example.bot.scheduler;

import org.example.bot.component.MessageLoader;
import org.example.bot.data.Message;
import org.example.bot.request.MessageRequest;
import org.example.bot.response.MessageResponse;
import org.example.bot.response.UpdateResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

import static org.example.bot.helper.AnalyzeMessageHelper.getLastUpdateId;

/**
 * Класс, который с определенной переодичностью получает не прочитанные сообщения
 * от сервера Telegram
 */
@Component
public class GetUpdatesScheduler {
    private static final Logger log = LoggerFactory.getLogger(GetUpdatesScheduler.class);

    @Value("${urlBotBrain}")
    private String urlBotBrain;

    private int currentMessageId;

    @Autowired
    private MessageLoader loader;

    private RestTemplate restTemplate;


    @PostConstruct
    private void setUp() {
        currentMessageId = 0;
        restTemplate = new RestTemplate();
    }

    @Scheduled(fixedRate = 5_000)
    public void getUpdate() {
        MessageResponse messages = loader.take(currentMessageId);

        currentMessageId = getLastUpdateId(messages) + 1;

        log.info("messages = " + messages);

        // Получаем сообщения и сразу отправляем их следующему микросервису,
        // пусть он решает что с ними делать
        messages.getResult().stream().map(UpdateResultResponse::getMessage)
                .map(message -> new MessageRequest(message.getFrom().getId(), message.getText()))
                .forEach(request -> restTemplate.postForObject(urlBotBrain + "sendMessage", request, Message.class));
    }
}