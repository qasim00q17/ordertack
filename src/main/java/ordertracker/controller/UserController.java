package ordertracker.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import ordertracker.entity.User;
import ordertracker.exception.BusinessException;
import ordertracker.repository.UserRepository;
import ordertracker.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile endpoints")
public class UserController {

    private final SecurityUtils  securityUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public UserProfileResponse getMyProfile() {
        User user = securityUtils.getCurrentUser();
        return UserProfileResponse.from(user);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User user = securityUtils.getCurrentUser();
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change current user password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = securityUtils.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BusinessException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Data
    public static class UpdateProfileRequest {
        @Size(min = 2, max = 100)
        private String fullName;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "Password must contain uppercase, lowercase and a digit")
        private String newPassword;
    }

    @Data
    public static class UserProfileResponse {
        private Long    id;
        private String  fullName;
        private String  email;
        private String  role;
        private Instant createdAt;

        public static UserProfileResponse from(User u) {
            UserProfileResponse r = new UserProfileResponse();
            r.id        = u.getId();
            r.fullName  = u.getFullName();
            r.email     = u.getEmail();
            r.role      = u.getRole().name();
            r.createdAt = u.getCreatedAt();
            return r;
        }
    }
}