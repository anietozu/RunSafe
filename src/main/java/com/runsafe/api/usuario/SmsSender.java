package com.runsafe.api.usuario;

public interface SmsSender {
    void enviar(String destinoE164, String texto);
}
