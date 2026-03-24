package de.elyseeevents.portal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@elysee-events.de}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTwoFactorCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Elysee Events - Ihr Verifizierungscode");
            message.setText(
                "Ihr Verifizierungscode lautet: " + code + "\n\n" +
                "Dieser Code ist 10 Minuten gueltig.\n\n" +
                "Falls Sie diesen Code nicht angefordert haben, ignorieren Sie diese E-Mail.\n\n" +
                "Elysee Event GmbH"
            );
            mailSender.send(message);
            log.info("2FA code sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send 2FA email to {}: {}", toEmail, e.getMessage());
        }
    }
}
