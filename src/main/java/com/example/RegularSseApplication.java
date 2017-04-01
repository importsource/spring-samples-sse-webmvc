package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.file.dsl.Files;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hezhuofan
 */
@SpringBootApplication
@RestController
public class RegularSseApplication {

    private final Map<String, SseEmitter> sses = new ConcurrentHashMap<>();


    @Bean
    public IntegrationFlow inboundFlow(@Value("${input-dir:file://${HOME}/Desktop/in}") File in) {
        System.out.println(in.getAbsolutePath());
        return IntegrationFlows.from(Files.inboundAdapter(in).autoCreateDirectory(true), poller -> poller.poller(spec -> spec.fixedRate(1000L)))
                .transform(File.class, File::getAbsolutePath).handle(String.class, (path, map) -> {
                    sses.forEach((key, sse) -> {
                                try {
                                    sse.send(path);
                                } catch (Exception ex) {
                                    throw new RuntimeException();
                                }
                            }

                    );
                    return null;
                })
                .channel(filesChannel())
                .get();
    }

    @Bean
    SubscribableChannel filesChannel() {
        return MessageChannels.publishSubscribe().get();
    }


    @GetMapping("/files/{name}")
    SseEmitter file(@PathVariable String name) {
        SseEmitter sseEmitter = new SseEmitter(60 * 1000L);
        sses.put(name, sseEmitter);
        return sseEmitter;

    }

    public static void main(String[] args) {
        SpringApplication.run(RegularSseApplication.class, args);
    }
}
