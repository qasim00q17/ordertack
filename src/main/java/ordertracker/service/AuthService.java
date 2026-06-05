package ordertracker.service;

import ordertracker.dto.request.LoginRequest;
import ordertracker.dto.request.RegisterRequest;
import ordertracker.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
}