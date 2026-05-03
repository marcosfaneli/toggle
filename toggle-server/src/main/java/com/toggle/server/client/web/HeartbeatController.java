package com.toggle.server.client.web;

import com.toggle.server.client.application.HeartbeatCommand;
import com.toggle.server.client.application.HeartbeatUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients/heartbeat")
public class HeartbeatController {

    private final HeartbeatUseCase heartbeatUseCase;

    public HeartbeatController(HeartbeatUseCase heartbeatUseCase) {
        this.heartbeatUseCase = heartbeatUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@Valid @RequestBody HeartbeatRequest request) {
        heartbeatUseCase.execute(new HeartbeatCommand(request.serviceName(), request.instanceId()));
    }
}
