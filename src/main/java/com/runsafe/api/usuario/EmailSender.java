package com.runsafe.api.usuario;

public interface EmailSender {
    void enviar(String destino, String asunto, String texto);
}
