package com.carpool.carpoolingapp.service;

import com.carpool.carpoolingapp.model.User;
import com.carpool.carpoolingapp.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority; // ADD THIS IMPORT
import org.springframework.security.core.authority.SimpleGrantedAuthority; // ADD THIS IMPORT
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List; // ADD THIS IMPORT

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        // CREATE A LIST OF AUTHORITIES (ROLES) FOR THE USER
        // Spring Security expects roles to be prefixed with "ROLE_"
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // RETURN A NEW USERDETAILS OBJECT WITH THE USER'S AUTHORITIES
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities // Pass the list of roles here
        );
    }
}