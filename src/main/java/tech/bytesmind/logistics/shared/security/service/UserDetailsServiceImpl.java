package tech.bytesmind.logistics.shared.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.bytesmind.logistics.auth.domain.model.Role;
import tech.bytesmind.logistics.auth.domain.model.User;
import tech.bytesmind.logistics.auth.infrastructure.repository.UserRepository;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Spring Security {@link UserDetailsService} backed by the local {@code platform_user} table.
 *
 * <p><b>Critical design decision:</b> {@link UserDetails#getUsername()} returns the user's
 * <em>UUID string</em>, not the login identifier. Spring Authorization Server uses
 * {@code Authentication.getName()} as the JWT {@code sub} claim. By returning the UUID
 * here, the JWT {@code sub} becomes the user UUID — exactly what
 * {@link SecurityContextService} expects when extracting the security context.
 *
 * <p>Login accepts either <strong>username</strong> or <strong>email</strong> as the identifier.
 *
 * <p>Account checks:
 * <ul>
 *   <li>Disabled if {@code active = false}</li>
 *   <li>Locked if {@code email_verified = false}</li>
 *   <li>Login fails immediately if no {@code password_hash} is set</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrUsernameWithRoles(identifier)
                .orElseThrow(() -> {
                    log.debug("Authentication attempt with unknown identifier: {}", identifier);
                    return new UsernameNotFoundException("Invalid credentials");
                });

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            log.warn("User '{}' has no password set — authentication will fail", user.getId());
        }

        Collection<GrantedAuthority> authorities = buildAuthorities(user);

        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),        // ← becomes JWT 'sub' claim
                user.getPasswordHash() != null ? user.getPasswordHash() : "",
                user.isActive(),                // enabled
                true,                           // accountNonExpired
                true,                           // credentialsNonExpired
                user.isEmailVerified(),         // accountNonLocked — blocked until email verified
                authorities
        );
    }

    private Collection<GrantedAuthority> buildAuthorities(User user) {
        return user.getRoles().stream()
                .filter(Role::isActive)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode().toUpperCase()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
