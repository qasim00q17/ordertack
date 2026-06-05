package ordertracker.dto.response;

import lombok.Builder;
import lombok.Data;
import ordertracker.enums.Role;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long   expiresIn;
    private Long   userId;
    private String email;
    private String fullName;
    private Role   role;
}