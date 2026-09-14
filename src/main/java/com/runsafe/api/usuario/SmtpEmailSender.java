package com.runsafe.api.usuario;

import com.runsafe.api.common.ApiException;
import jakarta.mail.AuthenticationFailedException;
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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public void enviar(String destino, String asunto, String texto) {
        String webhook = env("MAIL_WEBHOOK_URL", "");
        if (!webhook.isBlank()) {
            enviarWebhook(webhook, destino, asunto, texto);
            return;
        }
        enviarSmtp(destino, asunto, texto);
    }

    private void enviarWebhook(String webhook, String destino, String asunto, String texto) {
        try {
            String secret = env("MAIL_WEBHOOK_SECRET", "");
            String qs = "secret=" + enc(secret)
                    + "&to=" + enc(destino)
                    + "&subject=" + enc(asunto)
                    + "&body=" + enc(texto);
            String join = webhook.contains("?") ? "&" : "?";
            HttpRequest req = HttpRequest.newBuilder(URI.create(webhook + join + qs))
                    .timeout(Duration.ofSeconds(25))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            String body = res.body() == null ? "" : res.body();
            if (res.statusCode() >= 400 || body.toLowerCase().contains("forbidden")) {
                log.warn("Webhook email HTTP {}: {}", res.statusCode(), body);
                throw new ApiException("No se pudo enviar el email. Revisa MAIL_WEBHOOK_URL y MAIL_WEBHOOK_SECRET.");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Webhook email falló: {}", e.getMessage());
            throw new ApiException("No se pudo enviar el email. Inténtalo más tarde.");
        }
    }

    private void enviarSmtp(String destino, String asunto, String texto) {
        String host = env("MAIL_HOST", "smtp.gmail.com");
        String user = env("MAIL_USER", "");
        String pass = env("MAIL_PASSWORD", "").replace(" ", "");
        String from = env("MAIL_FROM", user);
        if (user.isBlank() || pass.isBlank() || from.isBlank()) {
            throw new ApiException("Configura MAIL_WEBHOOK_URL (Gmail por HTTPS). Render bloquea SMTP.");
        }
        Exception last = null;
        for (MailRoute route : routes(host)) {
            try {
                enviarPor(route, user, pass, from, destino, asunto, texto);
                return;
            } catch (AuthenticationFailedException e) {
                throw new ApiException("Gmail rechazó el usuario o la contraseña de aplicación.");
            } catch (Exception e) {
                log.warn("SMTP {} falló: {}", route.port, e.getMessage());
                last = e;
            }
        }
        String detail = last == null || last.getMessage() == null ? "" : last.getMessage();
        if (detail.toLowerCase().contains("timed out") || detail.toLowerCase().contains("timeout")) {
            throw new ApiException("Render bloquea SMTP. Usa MAIL_WEBHOOK_URL con un script de Google.");
        }
        throw new ApiException("No se pudo enviar el email. Revisa MAIL_USER y MAIL_PASSWORD.");
    }

    private void enviarPor(MailRoute route, String user, String pass, String from, String to, String asunto, String texto)
            throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", route.host);
        props.put("mail.smtp.port", String.valueOf(route.port));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");
        props.put("mail.smtp.writetimeout", "8000");
        props.put("mail.smtp.ssl.trust", route.host);
        if (route.ssl) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        }
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from, "RunSafe", "UTF-8"));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(asunto, "UTF-8");
        msg.setText(texto, "UTF-8");
        Transport.send(msg);
    }

    private static MailRoute[] routes(String host) {
        String custom = env("MAIL_PORT", "");
        if (!custom.isBlank()) {
            int port = Integer.parseInt(custom);
            return new MailRoute[]{new MailRoute(host, port, port == 465)};
        }
        return new MailRoute[]{
                new MailRoute(host, 465, true),
                new MailRoute(host, 587, false),
        };
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        return v.trim();
    }

    private record MailRoute(String host, int port, boolean ssl) {}
}
