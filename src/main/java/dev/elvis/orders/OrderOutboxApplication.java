package dev.elvis.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderOutboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderOutboxApplication.class, args);
    }
}
