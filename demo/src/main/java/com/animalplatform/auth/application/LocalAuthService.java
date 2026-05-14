package com.animalplatform.auth.application;

import com.animalplatform.user.domain.User;
import com.animalplatform.user.domain.UserRepository;
import com.animalplatform.user.domain.UserRole;
import com.animalplatform.user.domain.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class LocalAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(normalizeUsername(username));
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByName(normalizeNickname(nickname));
    }

    public User signUp(
            String username,
            String password,
            String name,
            String phone,
            String email,
            String birthDate,
            String gender,
            String address,
            boolean privacyConsent
    ) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedName = normalizeNickname(name);
        String normalizedPhone = normalizePhone(phone);
        String normalizedEmail = normalizeEmail(email);
        String normalizedBirthDate = normalizeOptionalDigits(birthDate, "생년월일", 8, 8);
        String normalizedGender = normalizeOptionalText(gender, 20);
        String normalizedAddress = normalizeOptionalText(address, 500);

        if (!privacyConsent) {
            throw new ResponseStatusException(BAD_REQUEST, "개인정보 수집 및 이용에 동의해주세요.");
        }

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByName(normalizedName)) {
            throw new ResponseStatusException(CONFLICT, "이미 사용 중인 닉네임입니다.");
        }
        if (normalizedEmail != null && userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        User user = new User(
                UserRole.MEMBER,
                normalizedUsername,
                normalizedEmail,
                passwordEncoder.encode(password),
                normalizedName,
                normalizedPhone,
                normalizedBirthDate,
                normalizedGender,
                normalizedAddress,
                privacyConsent,
                UserStatus.ACTIVE
        );

        return userRepository.save(user);
    }

    public User login(String username, String password, HttpServletRequest request, HttpServletResponse response) {
        String normalizedUsername = normalizeUsername(username);

        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        return user;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }

    private String normalizeUsername(String username) {
        String normalized = String.valueOf(username).trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "아이디를 입력해주세요.");
        }
        return normalized;
    }

    private String normalizeNickname(String nickname) {
        String normalized = String.valueOf(nickname).trim();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "닉네임을 입력해주세요.");
        }
        if (normalized.length() > 30) {
            throw new ResponseStatusException(BAD_REQUEST, "닉네임은 30자 이하로 입력해주세요.");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        String normalized = String.valueOf(phone).replaceAll("\\D", "");
        if (normalized.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "휴대전화 번호를 입력해주세요.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = String.valueOf(email).trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "이메일을 입력해주세요.");
        }
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ResponseStatusException(BAD_REQUEST, "이메일 형식을 확인해주세요.");
        }
        return normalized;
    }

    private String normalizeOptionalDigits(String value, String label, int minLength, int maxLength) {
        String normalized = String.valueOf(value == null ? "" : value).replaceAll("\\D", "");
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw new ResponseStatusException(BAD_REQUEST, label + " 형식을 확인해주세요.");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(BAD_REQUEST, "입력값이 너무 깁니다.");
        }
        return normalized;
    }
}
