package com.sigepes.backend.users;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sigepes.backend.users.UserDtos.CreateUserRequest;
import com.sigepes.backend.users.UserDtos.UpdateUserRequest;
import com.sigepes.backend.users.UserDtos.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-01 / RF-20 · Administración de usuarios. Solo ADMIN. */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista los usuarios del sistema")
    public List<UserResponse> list() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un usuario desde administración")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createByAdmin(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualiza rol, estado o nombre de un usuario")
    public UserResponse update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.update(id, request));
    }
}
