package com.medcare.clinic_backend.security;

import com.medcare.clinic_backend.entity.Account;
import com.medcare.clinic_backend.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay tai khoan: " + username));

        String rawRole = account.getRole() == null ? "" : account.getRole().trim().toUpperCase();
        String normalizedRole = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(normalizedRole));

        return new User(account.getUsername(), account.getPassword(), authorities);
    }
}
