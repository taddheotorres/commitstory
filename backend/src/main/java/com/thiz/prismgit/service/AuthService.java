package com.thiz.prismgit.service;

import com.thiz.prismgit.dto.AuthResponse;
import com.thiz.prismgit.dto.LoginRequest;
import com.thiz.prismgit.dto.RegisterRequest;
import com.thiz.prismgit.entity.User;
import com.thiz.prismgit.repository.UserRepository;
import com.thiz.prismgit.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        var user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        log.info("User registered: {} ({})", user.getName(), user.getEmail());
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User logged in: {} ({})", user.getName(), user.getEmail());
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        var token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(
                token,
                86400000L,
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
