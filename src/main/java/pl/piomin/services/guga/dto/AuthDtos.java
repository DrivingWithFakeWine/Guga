package pl.piomin.services.guga.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "须为 3-20 位字母、数字或下划线") String username,
            @NotBlank @Email(message = "邮箱格式不正确") String email,
            @NotBlank @Size(min = 8, max = 72, message = "密码长度须为 8-72 位") String password) {
    }

    public record LoginRequest(
            @NotBlank @Email(message = "邮箱格式不正确") String email,
            @NotBlank String password) {
    }

    public record UserResponse(Long id, String username, String email) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    private AuthDtos() {
    }
}
