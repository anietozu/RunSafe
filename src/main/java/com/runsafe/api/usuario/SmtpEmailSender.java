package com.runsafe.api.usuario;

import com.runsafe.api.common.ApiException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    @Override
    public void enviar(String destino, String asunto, String texto) {
        String host = env("MAIL_HOST", "smtp.gmail.com");
        String user = env("MAIL_USER", "");
        String pass = env("MAIL_PASSWORD", "");
        String from = env("MAIL_FROM", user);
        String port = env("MAIL_PORT", "587");
        if (user.isBlank() || pass.isBlank() || from.isBlank()) {
            throw new ApiException("El envío de email no está configurado. Faltan MAIL_USER y MAIL_PASSWORD.");
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.ssl.trust", host);
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from, "RunSafe", "UTF-8"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(destino));
            msg.setSubject(asunto, "UTF-8");
            msg.setText(texto, "UTF-8");
            Transport.send(msg);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error enviando email: {}", e.getMessage());
            throw new ApiException("No se pudo enviar el email. Inténtalo más tarde.");
        }
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        return v.trim();
    }
}
