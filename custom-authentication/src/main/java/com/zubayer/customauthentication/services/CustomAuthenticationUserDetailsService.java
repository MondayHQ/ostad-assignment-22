package com.zubayer.customauthentication.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

// Local Imports
import com.zubayer.customauthentication.models.UserEntity;
import com.zubayer.customauthentication.repositories.AuthRepository;

@Service
public class CustomAuthenticationUserDetailsService implements UserDetailsService {

    private final AuthRepository authRepository;

    public CustomAuthenticationUserDetailsService(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<UserEntity> user = authRepository.findByEmail(username);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        return user.get();

    }

}
