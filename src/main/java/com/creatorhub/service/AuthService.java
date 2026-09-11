package com.creatorhub.service;

import com.creatorhub.dto.auth.*;
import com.creatorhub.entity.User;
import com.creatorhub.repository.UserRepository;
import com.creatorhub.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) throw new IllegalArgumentException("Email already registered");
        User user = User.builder().name(request.name().trim()).email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password())).build();
        userRepository.save(user);
        return new AuthResponse(user.getUserId(), user.getName(), user.getEmail(), jwtService.generateToken(user.getEmail()));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElseThrow();
        return new AuthResponse(user.getUserId(), user.getName(), user.getEmail(), jwtService.generateToken(user.getEmail()));
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleTokenVerifierService.GoogleProfile google = googleTokenVerifierService.verify(request.credential());

        User user = userRepository.findByGoogleSubject(google.subject()).orElse(null);

        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(google.email()).orElse(null);

            if (user != null) {
                if (!google.googleAuthoritativeEmail()) {
                    throw new IllegalArgumentException("This email already has a CreatorStats password account. Sign in with your password first.");
                }
                user.setGoogleSubject(google.subject());
                if (google.name() != null && !google.name().isBlank()) {
                    user.setName(google.name().trim());
                }
                userRepository.save(user);
            } else {
                user = User.builder()
                        .name(google.name() == null || google.name().isBlank() ? google.email().split("@")[0] : google.name().trim())
                        .email(google.email())
                        .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                        .googleSubject(google.subject())
                        .build();
                userRepository.save(user);
            }
        }

        return new AuthResponse(user.getUserId(), user.getName(), user.getEmail(), jwtService.generateToken(user.getEmail()));
    }
}
