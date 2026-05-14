package com.animalplatform.auth.application;
import com.animalplatform.user.domain.User;
import com.animalplatform.user.domain.UserRepository;
import com.animalplatform.user.domain.UserRole;
import com.animalplatform.user.domain.UserStatus;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
@Service
public class AuthenticatedUserService {
    private final UserRepository userRepository;
    public AuthenticatedUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public User getCurrentUser() {
        return getCurrentUserOptional()
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Authentication required."));
    }

    public Optional<User> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String username) {
            return userRepository.findByUsername(username);
        }
        if (principal instanceof UserDetails userDetails) {
            return userRepository.findByUsername(userDetails.getUsername());
        }
        if (!(principal instanceof OAuth2User oauth2User)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unsupported authentication principal.");
        }
        String email = stringValue(oauth2User.getAttribute("email"));
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unable to resolve social login email.");
        }
        return Optional.of(userRepository.findByEmail(email)
                .orElseGet(() -> createMember(oauth2User, email)));
    }
    private User createMember(OAuth2User oauth2User, String email) {
        String name = stringValue(oauth2User.getAttribute("name"));
        String resolvedName = (name == null || name.isBlank()) ? "User" : name;
        return userRepository.save(new User(
                UserRole.MEMBER,
                email,
                email,
                "SOCIAL_LOGIN",
                resolvedName,
                null,
                UserStatus.ACTIVE
        ));
    }
    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        if (value instanceof Map<?, ?> mapValue) {
            Object nestedValue = mapValue.get("value");
            return nestedValue == null ? null : String.valueOf(nestedValue);
        }
        return String.valueOf(value);
    }
}
