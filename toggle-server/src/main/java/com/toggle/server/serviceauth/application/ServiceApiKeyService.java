package com.toggle.server.serviceauth.application;

import com.github.f4b6a3.ulid.UlidCreator;
import com.toggle.server.serviceauth.persistence.ServiceApiKeyPersistenceAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class ServiceApiKeyService {

    private final ServiceApiKeyPersistenceAdapter persistence;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public ServiceApiKeyService(ServiceApiKeyPersistenceAdapter persistence, Clock appClock) {
        this.persistence = persistence;
        this.clock = appClock;
    }

    @Transactional
    public CreatedServiceApiKeyView create(CreateServiceApiKeyCommand command) {
        var rawKey = generateRawKey();
        var now = Instant.now(clock);
        var view = persistence.create(
                UlidCreator.getMonotonicUlid().toString(),
                command.serviceName().trim(),
                command.name().trim(),
                hash(rawKey),
                now);

        return new CreatedServiceApiKeyView(
                view.id(),
                view.serviceName(),
                view.name(),
                rawKey,
                view.createdAt());
    }

    @Transactional(readOnly = true)
    public List<ServiceApiKeyView> list() {
        return persistence.findAll();
    }

    @Transactional
    public void revoke(String publicId) {
        persistence.revoke(publicId, Instant.now(clock));
    }

    @Transactional
    public ServiceApiKeyAuthentication authenticate(String rawKey) {
        var view = persistence.findActiveByHash(hash(rawKey))
                .orElseThrow(ServiceApiKeyAuthenticationException::new);
        persistence.markUsed(view.id(), Instant.now(clock));
        return new ServiceApiKeyAuthentication(view.id(), view.serviceName());
    }

    private String generateRawKey() {
        var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "swb_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawKey) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
