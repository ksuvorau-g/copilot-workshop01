package com.example.aidemo1.security;

import com.example.aidemo1.entity.User;
import com.example.aidemo1.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 * Loads user-specific data from the database for authentication and authorization.
 * 
 * <p>This service:</p>
 * <ul>
 *   <li>Fetches user data from the database by username</li>
 *   <li>Converts User entity to Spring Security's UserDetails</li>
 *   <li>Maps roles to granted authorities</li>
 *   <li>Supports role-based access control via @PreAuthorize</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads user by username for authentication.
     * 
     * <p>This method is called by Spring Security during authentication.
     * It fetches the user from the database and converts it to UserDetails.</p>
     *
     * @param username the username identifying the user whose data is required
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if the user could not be found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
        
        log.debug("User found: {}, enabled: {}, roles: {}", 
                user.getUsername(), user.isEnabled(), user.getRoles().size());
        
        return buildUserDetails(user);
    }

    /**
     * Converts User entity to Spring Security UserDetails.
     * 
     * @param user the user entity
     * @return UserDetails implementation
     */
    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities(mapRolesToAuthorities(user))
                .build();
    }

    /**
     * Maps user roles to Spring Security GrantedAuthority objects.
     * 
     * <p>Role names from the database are prefixed with "ROLE_" if not already present.
     * For example:</p>
     * <ul>
     *   <li>"ADMIN" → "ROLE_ADMIN"</li>
     *   <li>"ROLE_USER" → "ROLE_USER" (unchanged)</li>
     * </ul>
     *
     * @param user the user entity
     * @return collection of granted authorities
     */
    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(User user) {
        return user.getRoles().stream()
                .map(role -> {
                    String roleName = role.getName();
                    // Ensure role name has ROLE_ prefix for Spring Security
                    if (!roleName.startsWith("ROLE_")) {
                        roleName = "ROLE_" + roleName;
                    }
                    log.debug("Mapping role: {} → {}", role.getName(), roleName);
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toList());
    }
}
