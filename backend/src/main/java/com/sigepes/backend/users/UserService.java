package com.sigepes.backend.users;

import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigepes.backend.audit.AuditActionType;
import com.sigepes.backend.audit.AuditService;
import com.sigepes.backend.common.ApiException;
import com.sigepes.backend.common.ErrorCode;
import com.sigepes.backend.common.Ids;
import com.sigepes.backend.users.UserDtos.CreateUserRequest;
import com.sigepes.backend.users.UserDtos.UpdateUserRequest;

/** RF-01 · Gestión de usuarios, identidad y roles. */
@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder, AuditService auditService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserEntity> findAll() {
        return repository.findAllByOrderByFullNameAsc();
    }

    @Transactional(readOnly = true)
    public UserEntity byId(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserEntity byEmail(String email) {
        return repository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> ApiException.of(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Crea un usuario aplicando RN-01: un documento de identidad no puede registrarse más de una
     * vez. El correo también es único porque es la credencial de acceso.
     */
    @Transactional
    public UserEntity create(String nationalId, String fullName, String email, Role role, String rawPassword,
            boolean demo) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedNationalId = nationalId.trim();

        if (repository.existsByNationalId(normalizedNationalId)) {
            throw ApiException.of(ErrorCode.DOCUMENT_ALREADY_REGISTERED);
        }
        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw ApiException.of(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        UserEntity user = new UserEntity(
                Ids.generate("usr"),
                normalizedNationalId,
                fullName.trim(),
                normalizedEmail,
                passwordEncoder.encode(rawPassword),
                role);
        user.setDemo(demo);
        return repository.save(user);
    }

    /** Alta desde administración (auditada aparte del auto-registro). */
    @Transactional
    public UserEntity createByAdmin(CreateUserRequest request) {
        UserEntity user = create(request.nationalId(), request.fullName(), request.email(),
                Role.valueOf(request.role()), request.password(), false);
        auditService.record(AuditActionType.CREATE, "USER_CREATED", AuditService.MODULE_USERS, "Usuario",
                user.getId(), "Alta administrativa de " + user.getEmail() + " con rol " + user.getRole());
        return user;
    }

    @Transactional
    public UserEntity update(String id, UpdateUserRequest request) {
        UserEntity user = byId(id);
        String before = describe(user);

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.role() != null && !request.role().isBlank()) {
            user.setRole(Role.valueOf(request.role()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            user.setStatus(UserStatus.valueOf(request.status()));
        }

        UserEntity saved = repository.save(user);
        auditService.recordChange("USER_UPDATED", AuditService.MODULE_USERS, "Usuario", saved.getId(),
                "Actualización administrativa de " + saved.getEmail(), before, describe(saved));
        return saved;
    }

    private static String describe(UserEntity user) {
        return AuditService.toJson(Map.of(
                "fullName", user.getFullName(),
                "role", user.getRole().name(),
                "status", user.getStatus().name()));
    }
}
