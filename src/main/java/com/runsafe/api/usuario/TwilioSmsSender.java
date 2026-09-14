package com.runsafe.api.usuario;

import com.runsafe.api.common.ApiException;
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
import java.util.Base64;

@Component
public class TwilioSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsSender.class);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String sid;
    private final String token;
    private final String from;

    public TwilioSmsSender() {
        this.sid = env("TWILIO_ACCOUNT_SID");
        this.token = env("TWILIO_AUTH_TOKEN");
        this.from = env("TWILIO_FROM");
    }

    TwilioSmsSender(String sid, String token, String from) {
        this.sid = sid == null ? "" : sid.trim();
        this.token = token == null ? "" : token.trim();
        this.from = from == null ? "" : from.trim();
    }

    private static String env(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v.trim();
    }

    @Override
    public void enviar(String destinoE164, String texto) {
        if (sid.isBlank() || token.isBlank() || from.isBlank()) {
            throw new ApiException("El envío de SMS no está configurado. Faltan TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN y TWILIO_FROM.");
        }
        try {
            String body = "To=" + enc(destinoE164) + "&From=" + enc(from) + "&Body=" + enc(texto);
            String auth = Base64.getEncoder().encodeToString((sid + ":" + token).getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + sid + "/Messages.json"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 300) {
                log.warn("Twilio respondió {}", res.statusCode());
                throw new ApiException("No se pudo enviar el SMS. Comprueba el número o inténtalo más tarde.");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error enviando SMS: {}", e.getMessage());
            throw new ApiException("No se pudo enviar el SMS. Inténtalo más tarde.");
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
