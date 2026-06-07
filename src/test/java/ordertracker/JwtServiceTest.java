package ordertracker;

import ordertracker.security.JwtProperties;
import ordertracker.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService    jwtService;
    private UserDetails   userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        props.setExpirationMs(3600000L);
        props.setRefreshExpirationMs(86400000L);

        jwtService  = new JwtService(props);
        userDetails = User.withUsername("test@example.com")
                .password("pw")
                .authorities(List.of())
                .build();
    }

    @Test
    void generateToken_extractUsername_matches() {
        String token    = jwtService.generateToken(userDetails);
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_wrongUser_returnsFalse() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = User.withUsername("other@example.com")
                .password("pw").authorities(List.of()).build();
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void generateRefreshToken_isNotExpired() {
        String token = jwtService.generateRefreshToken(userDetails);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }
}