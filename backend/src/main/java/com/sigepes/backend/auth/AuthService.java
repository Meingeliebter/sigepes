package com.sigepes.backend.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.auth.AuthModels.LoginRequest;
import com.sigepes.backend.auth.AuthModels.RegisterRequest;
import com.sigepes.backend.auth.AuthModels.Session;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.security.JwtTokenProvider;
import com.sigepes.backend.users.Role;
import com.sigepes.backend.users.UserDtos.UserResponse;
import com.sigepes.backend.users.UserEntity;
import com.sigepes.backend.users.UserRepository;
import com.sigepes.backend.users.UserService;
import com.sigepes.backend.users.UserStatus;

/** RF-01 · Registro, autenticación y sesión. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider, AuditService auditService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    @Transactional
    public Session login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(request.email().trim()).orElse(null);

        // Se compara siempre contra un hash para no filtrar por tiempo si el correo existe o no.
        boolean valid = user != null && passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!valid) {
            auditService.recordAs(user == null ? null : user.getId(),
                    user == null ? request.email().trim() : user.getFullName(),
                    AuditActionType.SECURITY, "LOGIN_FAILED", AuditService.MODULE_AUTH, "Usuario",
                    user == null ? null : user.getId(),
                    "Intento de inicio de sesión fallido para " + request.email().trim(), null, null);
            throw ApiException.of(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            auditService.recordAs(user.getId(), user.getFullName(), AuditActionType.SECURITY, "LOGIN_BLOCKED",
                    AuditService.MODULE_AUTH, "Usuario", user.getId(), "Inicio de sesión de una cuenta suspendida",
                    null, null);
            throw ApiException.of(ErrorCode.ACCOUNT_SUSPENDED);
        }

        auditService.recordAs(user.getId(), user.getFullName(), AuditActionType.SECURITY, "LOGIN",
                AuditService.MODULE_AUTH, "Usuario", user.getId(), "Inicio de sesión correcto", null, null);
        return sessionFor(user);
    }

    @Transactional
    public Session register(RegisterRequest request) {
        UserEntity user = userService.create(request.nationalId(), request.fullName(), request.email(),
                Role.valueOf(request.role()), request.password(), false);

        auditService.recordAs(user.getId(), user.getFullName(), AuditActionType.CREATE, "USER_REGISTERED",
                AuditService.MODULE_AUTH, "Usuario", user.getId(), "Auto-registro con rol " + user.getRole(), null,
                null);
        return sessionFor(user);
    }

    @Transactional(readOnly = true)
    public Session sessionForId(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.of(ErrorCode.UNAUTHORIZED));
        return sessionFor(user);
    }

    @Transactional
    public void logout(String userId, String fullName) {
        auditService.recordAs(userId, fullName, AuditActionType.SECURITY, "LOGOUT", AuditService.MODULE_AUTH,
                "Usuario", userId, "Cierre de sesión", null, null);
    }

    private Session sessionFor(UserEntity user) {
        String token = tokenProvider.createToken(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().name());
        return new Session(token, "Bearer", tokenProvider.expirationSeconds(), UserResponse.from(user));
    }
}
