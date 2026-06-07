package ordertracker;

import ordertracker.dto.request.LoginRequest;
import ordertracker.dto.request.RegisterRequest;
import ordertracker.dto.response.AuthResponse;
import ordertracker.entity.User;
import ordertracker.enums.Role;
import ordertracker.exception.BusinessException;
import ordertracker.repository.UserRepository;
import ordertracker.security.JwtProperties;
import ordertracker.security.JwtService;
import ordertracker.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository        userRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock JwtService            jwtService;
    @Mock JwtProperties         jwtProperties;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService    userDetailsService;

    @InjectMocks AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("encoded-password")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test User");
        req.setEmail("test@example.com");
        req.setPassword("Password1!");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDetails mockDetails = org.springframework.security.core.userdetails.User
                .withUsername("test@example.com").password("encoded")
                .authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockDetails);
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProperties.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsBusinessException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("Password1!");
        req.setFullName("Test");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("Password1!");

        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("test@example.com", null, List.of()));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        UserDetails mockDetails = org.springframework.security.core.userdetails.User
                .withUsername("test@example.com").password("encoded")
                .authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockDetails);
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProperties.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRole()).isEqualTo(Role.USER);
    }
}