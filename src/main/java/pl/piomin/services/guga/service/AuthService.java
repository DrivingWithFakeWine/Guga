package pl.piomin.services.guga.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.piomin.services.guga.common.ResourceNotFoundException;
import pl.piomin.services.guga.dto.AuthDtos.AuthResponse;
import pl.piomin.services.guga.dto.AuthDtos.LoginRequest;
import pl.piomin.services.guga.dto.AuthDtos.RegisterRequest;
import pl.piomin.services.guga.dto.AuthDtos.UserResponse;
import pl.piomin.services.guga.common.DuplicateResourceException;
import pl.piomin.services.guga.entity.User;
import pl.piomin.services.guga.repo.UserRepository;
import pl.piomin.services.guga.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("用户名已被占用");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return new AuthResponse(jwtService.generate(user.getUsername()), toResponse(user));
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("邮箱或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("邮箱或密码错误");
        }
        return new AuthResponse(jwtService.generate(user.getUsername()), toResponse(user));
    }

    public UserResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("未登录或登录已过期");
        }
        return userRepository.findByUsername(authentication.getName())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
