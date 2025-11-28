package org.example.service;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.example.dao.UserRepository;
import org.example.dto.AuthResponse;
import org.example.dto.RegisterResponse;
import org.example.entity.User;
import org.example.exception.NameAlreadyExistsException;
import org.example.exception.InvalidPassword;
import org.example.exception.UserNotFoundException;
import org.example.tools.PasswordEncoder;

@Stateless
public class AuthService {

    @EJB
    private UserRepository userRepository;

    @EJB
    private RefreshTokenService refreshTokenService;

    @EJB
    private PasswordEncoder passwordEncoder;

    @EJB
    private JWTService jwtService;

    public AuthResponse authUser(String name, String password) {
        User user = userRepository.findByName(name)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с таким именем не найден"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPassword(name);
        }


        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(15 * 60L)
                .userId(user.getId())
                .name(user.getName())
                .build();
    }

    public RegisterResponse registerUser(String name, String password) {
        if (userRepository.findByName(name).isPresent()) {
            throw new NameAlreadyExistsException(name);
        }

        User user = User.builder()
                .name(name)
                .password(passwordEncoder.encode(password))
                .verified(true)
                .build();

        userRepository.save(user);

        return RegisterResponse.builder()
                .name(name)
                .message("Регистрация успешно завершена")
                .build();
    }

    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }
}