package com.runsafe.api.usuario;

public record AuthResponse(String token, UsuarioResponse usuario) {}
