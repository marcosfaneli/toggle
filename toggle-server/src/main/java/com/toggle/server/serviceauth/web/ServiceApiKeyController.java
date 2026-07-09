package com.toggle.server.serviceauth.web;

import com.toggle.server.serviceauth.application.CreateServiceApiKeyCommand;
import com.toggle.server.serviceauth.application.CreatedServiceApiKeyView;
import com.toggle.server.serviceauth.application.ServiceApiKeyService;
import com.toggle.server.serviceauth.application.ServiceApiKeyView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/service-api-keys")
public class ServiceApiKeyController {

    private final ServiceApiKeyService serviceApiKeyService;

    public ServiceApiKeyController(ServiceApiKeyService serviceApiKeyService) {
        this.serviceApiKeyService = serviceApiKeyService;
    }

    @GetMapping
    public List<ServiceApiKeyView> list() {
        return serviceApiKeyService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedServiceApiKeyView create(@Valid @RequestBody CreateServiceApiKeyRequest request) {
        return serviceApiKeyService.create(new CreateServiceApiKeyCommand(request.serviceName(), request.name()));
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String publicId) {
        serviceApiKeyService.revoke(publicId);
    }
}
