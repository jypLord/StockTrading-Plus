package com.jypLord;



import com.jypLord.api.handler.LsBrokerClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


@SpringBootApplication
public class AutoInvestApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoInvestApplication.class, args);
    }
}
