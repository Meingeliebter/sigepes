package com.sigepes.backend.closures;

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

import com.sigepes.backend.closures.ClosureDtos.ClosureResponse;
import com.sigepes.backend.closures.ClosureDtos.CreateClosureRequest;
import com.sigepes.backend.closures.ClosureDtos.UpdateClosureRequest;
import com.sigepes.backend.common.AppClock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** RF-05 · Vedas y temporadas. */
@RestController
@RequestMapping("/api/v1/vedas")
@Tag(name = "Vedas")
public class ClosureController {

    private final ClosureService closureService;

    public ClosureController(ClosureService closureService) {
        this.closureService = closureService;
    }

    @GetMapping
    @Operation(summary = "Lista todas las vedas (programadas, vigentes y finalizadas)")
    public List<ClosureResponse> list() {
        return closureService.findAll();
    }

    @GetMapping("/activas")
    @Operation(summary = "Lista únicamente las vedas vigentes hoy")
    public List<ClosureResponse> active() {
        return closureService.findInForce();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','INSPECTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Declara una veda para una especie, zona y periodo")
    public ClosureResponse create(@Valid @RequestBody CreateClosureRequest request) {
        return ClosureResponse.from(closureService.create(request), AppClock.today());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','INSPECTOR')")
    @Operation(summary = "Reprograma o levanta una veda")
    public ClosureResponse update(@PathVariable String id, @Valid @RequestBody UpdateClosureRequest request) {
        return ClosureResponse.from(closureService.update(id, request), AppClock.today());
    }
}
