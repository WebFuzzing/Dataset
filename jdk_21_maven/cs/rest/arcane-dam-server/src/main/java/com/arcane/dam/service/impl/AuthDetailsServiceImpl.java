package com.arcane.dam.service.impl;

import com.arcane.dam.dto.UserPrincipal;
import com.arcane.dam.entity.Users;
import com.arcane.dam.repository.AuthRepository;
import com.arcane.dam.service.AuthDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthDetailsServiceImpl implements AuthDetailsService {

    private final AuthRepository authRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users authUser = authRepository.findUserByEmail(username);

        if(authUser == null) {
            throw new UsernameNotFoundException(username);
        }
        return new UserPrincipal(authUser);
    }
}
