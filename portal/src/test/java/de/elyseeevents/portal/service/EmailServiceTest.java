package de.elyseeevents.portal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private EmailService service;
    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);
        service = new EmailService(mailSender, templateEngine);
    }

    @Test
    void rejectsNullRecipient() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmail(null, "Hi", "tpl", Map.of()));
        verifyNoInteractions(mailSender);
    }

    @Test
    void rejectsCrlfInRecipient() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmail("victim@example.com\nBcc: attacker@evil.com",
                        "Subject", "tpl", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmail("victim@example.com\r\nBcc: attacker@evil.com",
                        "Subject", "tpl", Map.of()));
        verifyNoInteractions(mailSender);
    }

    @Test
    void rejectsUrlEncodedCrlfInRecipient() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmail("victim@example.com%0ABcc:attacker",
                        "Subject", "tpl", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmail("victim@example.com%0D%0AX-Mailer: evil",
                        "Subject", "tpl", Map.of()));
        // Lowercase variants
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmail("victim@example.com%0a",
                        "Subject", "tpl", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmail("victim@example.com%0d",
                        "Subject", "tpl", Map.of()));
    }

    @Test
    void rejectsCrlfInSubject() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmail("victim@example.com",
                        "Normal Subject\nBcc: attacker@evil.com", "tpl", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmail("victim@example.com",
                        "Subject%0aX-Forwarded: evil", "tpl", Map.of()));
        verifyNoInteractions(mailSender);
    }

    @Test
    void rejectsCrlfInAttachmentFilename() {
        assertThrows(IllegalArgumentException.class,
                () -> service.sendHtmlEmailWithAttachment("victim@example.com", "Subject",
                        "tpl", Map.of(), new byte[]{1, 2, 3},
                        "evil\nContent-Disposition: attachment.pdf"));
        verifyNoInteractions(mailSender);
    }
}
