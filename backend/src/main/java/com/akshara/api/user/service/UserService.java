package com.akshara.api.user.service;

import com.akshara.api.auth.dto.UserResponse;
import com.akshara.api.auth.exception.InvalidAccessTokenException;
import com.akshara.api.user.entity.AppUser;
import com.akshara.api.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String subject) {
        return UserResponse.from(getCurrentUserEntity(subject));
    }

    @Transactional(readOnly = true)
    public AppUser getCurrentUserEntity(String subject) {
        Long userId;

        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new InvalidAccessTokenException();
        }

        return userRepository.findById(userId)
                .filter(AppUser::isEnabled)
                .orElseThrow(InvalidAccessTokenException::new);
    }
}
