package com.runsafe.api.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        String env = firstNonBlank(System.getenv("DATABASE_URL"), System.getenv("JDBC_URL"));
        if (env == null) {
            throw new IllegalStateException(
                    "Falta DATABASE_URL. En Render, enlaza el PostgreSQL al web service.");
        }
        if (env.startsWith("jdbc:")) {
            return DataSourceBuilder.create()
                    .url(withSsl(env))
                    .username(System.getenv("DB_USER"))
                    .password(System.getenv("DB_PASSWORD"))
                    .build();
        }
        String stripped = env.replaceFirst("^postgres(ql)?://", "");
        int at = stripped.lastIndexOf('@');
        if (at < 0) {
            throw new IllegalStateException("DATABASE_URL no es una URL de PostgreSQL válida");
        }
        String userInfo = stripped.substring(0, at);
        String hostPart = stripped.substring(at + 1);
        int colon = userInfo.indexOf(':');
        String user = colon < 0 ? userInfo : userInfo.substring(0, colon);
        String password = colon < 0 ? "" : userInfo.substring(colon + 1);
        user = URLDecoder.decode(user, StandardCharsets.UTF_8);
        password = URLDecoder.decode(password, StandardCharsets.UTF_8);

        int slash = hostPart.indexOf('/');
        String hostPort = slash < 0 ? hostPart : hostPart.substring(0, slash);
        String dbAndQuery = slash < 0 ? "" : hostPart.substring(slash);
        String jdbc = withSsl("jdbc:postgresql://" + hostPort + dbAndQuery);

        return DataSourceBuilder.create()
                .url(jdbc)
                .username(user)
                .password(password)
                .build();
    }

    private static String withSsl(String jdbc) {
        if (jdbc.contains("sslmode=")) {
            return jdbc;
        }
        return jdbc.contains("?") ? jdbc + "&sslmode=require" : jdbc + "?sslmode=require";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
