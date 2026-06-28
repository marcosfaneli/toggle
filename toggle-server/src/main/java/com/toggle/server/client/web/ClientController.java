package com.toggle.server.client.web;

import com.toggle.server.client.application.DeregisterClientUseCase;
import com.toggle.server.client.application.ClientView;
import com.toggle.server.client.application.ListClientsUseCase;
import com.toggle.server.client.application.RegisterClientUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    private final RegisterClientUseCase registerClientUseCase;
    private final DeregisterClientUseCase deregisterClientUseCase;
    private final ListClientsUseCase listClientsUseCase;
    private final RegisterClientCommandMapper mapper;

    public ClientController(RegisterClientUseCase registerClientUseCase,
                            DeregisterClientUseCase deregisterClientUseCase,
                            ListClientsUseCase listClientsUseCase,
                            RegisterClientCommandMapper mapper) {
        this.registerClientUseCase = registerClientUseCase;
        this.deregisterClientUseCase = deregisterClientUseCase;
        this.listClientsUseCase = listClientsUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ClientView> list(@RequestParam(required = false) String serviceName) {
        if (serviceName != null && serviceName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceName must not be blank");
        }

        return listClientsUseCase.execute(serviceName);
    }

    @PostMapping("/register")
    public RegisterClientResponse register(@Valid @RequestBody RegisterClientRequest request) {
        log.info(
                "event=client_register_requested serviceName={} instanceId={} namespace={} podName={}",
                request.serviceName(),
                request.instanceId(),
                request.namespace(),
                request.podName());
        return RegisterClientResponse.from(registerClientUseCase.execute(mapper.toCommand(request)));
    }

    @DeleteMapping("/register/{instanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deregister(@PathVariable String instanceId,
                           @RequestParam @NotBlank String serviceName) {
        log.info("event=client_deregister_requested serviceName={} instanceId={}", serviceName, instanceId);
        deregisterClientUseCase.execute(serviceName, instanceId);
    }
}
