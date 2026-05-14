package com.animalplatform;

import jakarta.mail.internet.MimeMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@Profile("test")
class TestMailConfig {

    @Bean
    @Primary
    JavaMailSender testMailSender() {
        return new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mimeMessage) {
                // Tests should verify application behavior without requiring a local SMTP server.
            }

            @Override
            public void send(MimeMessage... mimeMessages) {
                // Tests should verify application behavior without requiring a local SMTP server.
            }
        };
    }
}
