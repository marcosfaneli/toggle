package com.toggle.server.client.web;

import com.toggle.server.client.application.DeregisterClientUseCase;
import com.toggle.server.client.application.RegisterClientUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients/register")
public class ClientController {

    private final RegisterClientUseCase registerClientUseCase;
    private final DeregisterClientUseCase deregisterClientUseCase;
    private final RegisterClientCommandMapper mapper;

    public ClientController(RegisterClientUseCase registerClientUseCase,
                            DeregisterClientUseCase deregisterClientUseCase,
                            RegisterClientCommandMapper mapper) {
        this.registerClientUseCase = registerClientUseCase;
        this.deregisterClientUseCase = deregisterClientUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public RegisterClientResponse register(@Valid @RequestBody RegisterClientRequest request) {
        return RegisterClientResponse.from(registerClientUseCase.execute(mapper.toCommand(request)));
    }

    @DeleteMapping("/{instanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deregister(@PathVariable String instanceId,
                           @RequestParam @NotBlank String serviceName) {
        deregisterClientUseCase.execute(serviceName, instanceId);
    }
}
