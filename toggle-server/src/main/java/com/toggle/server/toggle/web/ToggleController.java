package com.toggle.server.toggle.web;

import com.toggle.server.toggle.application.CreateToggleCommand;
import com.toggle.server.toggle.application.CreateToggleUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/toggles")
public class ToggleController {

    private final CreateToggleUseCase createToggleUseCase;

    public ToggleController(CreateToggleUseCase createToggleUseCase) {
        this.createToggleUseCase = createToggleUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ToggleResponse create(@Valid @RequestBody CreateToggleRequest request) {
        var command = new CreateToggleCommand(
                request.name(),
                request.ownerServiceName(),
                request.enabled());

        return ToggleResponse.from(createToggleUseCase.execute(command));
    }
}
