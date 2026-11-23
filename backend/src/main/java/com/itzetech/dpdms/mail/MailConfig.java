package com.itzetech.dpdms.mail;

import com.itzetech.dpdms.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;

@Configuration
class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    MailService mailService(ObjectProvider<JavaMailSender> sender, AppProperties props,
                            @Value("${spring.mail.host:}") String host) {
        JavaMailSender smtp = sender.getIfAvailable();
        if (smtp == null || host.isBlank()) {
            log.info("No mail server configured (MAIL_HOST is empty): e-mails will be written to the log.");
            return (to, subject, body) -> log.info("""

                    ---- E-MAIL (not sent: no mail server configured) ----
                    To: {}
                    Subject: {}

                    {}
                    -------------------------------------------------------""", to, subject, body);
        }
        return (to, subject, body) -> {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(props.mailFrom());
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            smtp.send(m);
        };
    }
}
