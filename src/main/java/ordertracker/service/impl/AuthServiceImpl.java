package ordertracker.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ordertracker.dto.request.LoginRequest;
import ordertracker.dto.request.RegisterRequest;
import ordertracker.dto.response.AuthResponse;
import ordertracker.entity.User;
import ordertracker.enums.Role;
import ordertracker.exception.BusinessException;
import ordertracker.repository.UserRepository;
import ordertracker.security.JwtProperties;
import ordertracker.security.JwtService;
import ordertracker.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final JwtProperties         jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService    userDetailsService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        return buildAuthResponse(user, details);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("User not found"));

        UserDetails details = userDetailsService.loadUserByUsername(user.getEmail());
        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user, details);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractUsername(refreshToken);
        UserDetails details = userDetailsService.loadUserByUsername(email);

        if (!jwtService.isTokenValid(refreshToken, details)) {
            throw new BusinessException("Invalid or expired refresh token");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found"));

        return buildAuthResponse(user, details);
    }

    private AuthResponse buildAuthResponse(User user, UserDetails details) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateToken(details))
                .refreshToken(jwtService.generateRefreshToken(details))
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpirationMs())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}