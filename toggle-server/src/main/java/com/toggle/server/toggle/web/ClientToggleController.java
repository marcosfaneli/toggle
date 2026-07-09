package com.toggle.server.toggle.web;

import com.toggle.server.serviceauth.web.ServiceIdentityVerifier;
import com.toggle.server.toggle.application.GetToggleByNameUseCase;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client-toggles")
public class ClientToggleController {

    private final GetToggleByNameUseCase getToggleByNameUseCase;
    private final ServiceIdentityVerifier serviceIdentityVerifier;

    public ClientToggleController(
            GetToggleByNameUseCase getToggleByNameUseCase,
            ServiceIdentityVerifier serviceIdentityVerifier) {
        this.getToggleByNameUseCase = getToggleByNameUseCase;
        this.serviceIdentityVerifier = serviceIdentityVerifier;
    }

    @GetMapping("/{name}")
    public ToggleResponse getByName(
            @PathVariable String name,
            @RequestParam String serviceName,
            Authentication authentication) {
        serviceIdentityVerifier.verify(authentication, serviceName);
        return ToggleResponse.from(getToggleByNameUseCase.execute(name));
    }
}
