package com.twekl.dashboard.service;

import com.twekl.dashboard.model.Admin;
import com.twekl.dashboard.model.AppUser;
import com.twekl.dashboard.repository.AdminRepository;
import com.twekl.dashboard.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final AppUserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(AdminRepository adminRepository, AppUserRepository userRepository) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be empty");
        }

        String cleanUsername = username.trim();

        // 1. Check in Admins
        Optional<Admin> adminOpt = adminRepository.findByUsernameIgnoreCase(cleanUsername);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
                throw new DisabledException("Admin account is disabled");
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            if (admin.isSuperAdmin()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            if (admin.isCanCreateRoles()) {
                authorities.add(new SimpleGrantedAuthority("PERMISSION_CREATE_ROLES"));
            }
            if (admin.isCanManageUsers()) {
                authorities.add(new SimpleGrantedAuthority("PERMISSION_MANAGE_USERS"));
            }

            return new org.springframework.security.core.userdetails.User(
                    admin.getUsername(),
                    admin.getPassword(),
                    authorities
            );
        }

        // 2. Check in AppUsers
        Optional<AppUser> userOpt = userRepository.findByUsernameEnIgnoreCase(cleanUsername);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new DisabledException("User account is disabled");
            }

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            return new org.springframework.security.core.userdetails.User(
                    user.getUsernameEn(),
                    user.getPassword(),
                    authorities
            );
        }

        throw new UsernameNotFoundException("No active admin or user found with identifier: " + username);
    }
}
