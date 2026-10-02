package com.twekl.dashboard.modules.auth.service;

import com.twekl.dashboard.modules.admin.entity.Admin;
import com.twekl.dashboard.modules.admin.repository.AdminRepository;
import com.twekl.dashboard.modules.user.entity.AppUser;
import com.twekl.dashboard.modules.user.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final AppUserRepository userRepository;

    public CustomUserDetailsService(AdminRepository adminRepository, AppUserRepository userRepository) {
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            if (admin.isSuperAdmin()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
            }
            return new User(admin.getUsername(), admin.getPassword(), authorities);
        }

        Optional<AppUser> userOpt = userRepository.findByUsernameEn(username);
        if (userOpt.isPresent()) {
            AppUser user = userOpt.get();
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            return new User(user.getUsernameEn(), user.getPassword(), authorities);
        }

        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
