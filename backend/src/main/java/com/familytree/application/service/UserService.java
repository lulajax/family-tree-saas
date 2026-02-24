package com.familytree.application.service;

import com.familytree.application.dto.UserDTO;
import com.familytree.application.dto.request.LoginRequest;
import com.familytree.application.dto.request.RegisterRequest;
import com.familytree.application.dto.response.LoginResponse;
import com.familytree.domain.User;
import com.familytree.infrastructure.repository.UserRepository;
import com.familytree.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("手机号已注册");
        }
        
        User user = User.builder()
            .phone(request.getPhone())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .nickname(request.getNickname())
            .build();
        
        user = userRepository.save(user);
        
        String token = jwtTokenProvider.generateToken(user.getId());
        
        return LoginResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .expiresIn(86400L)
            .user(toDTO(user))
            .build();
    }
    
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
            .orElseThrow(() -> new BadCredentialsException("手机号或密码错误"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("手机号或密码错误");
        }
        
        String token = jwtTokenProvider.generateToken(user.getId());
        
        return LoginResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .expiresIn(86400L)
            .user(toDTO(user))
            .build();
    }
    
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return toDTO(user);
    }
    
    @Transactional
    public UserDTO updateUser(UUID userId, String nickname, String avatarUrl) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        
        user = userRepository.save(user);
        return toDTO(user);
    }
    
    private UserDTO toDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .phone(user.getPhone())
            .nickname(user.getNickname())
            .avatarUrl(user.getAvatarUrl())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
