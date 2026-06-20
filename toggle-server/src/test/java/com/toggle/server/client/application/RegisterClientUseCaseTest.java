package com.toggle.server.client.application;

import com.toggle.server.client.domain.ClientInstance;
import com.toggle.server.client.domain.ClientInstanceStatus;
import com.toggle.server.client.domain.InvalidCallbackUrlException;
import com.toggle.server.client.persistence.ClientPersistenceAdapter;
import com.toggle.server.toggle.domain.Toggle;
import com.toggle.server.toggle.persistence.TogglePersistenceAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterClientUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-20T18:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ClientPersistenceAdapter clientPersistenceAdapter;

    @Mock
    private TogglePersistenceAdapter togglePersistenceAdapter;

    @Test
    void execute_whenLocalCallbackAndNotAllowed_throwsInvalidCallbackUrl() {
        var useCase = new RegisterClientUseCase(clientPersistenceAdapter, togglePersistenceAdapter, CLOCK, false);
        var command = commandWithCallback("http://localhost:8081/callback");

        when(togglePersistenceAdapter.findAllByNames(List.of("novo-checkout")))
                .thenReturn(List.of(aToggle("novo-checkout")));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidCallbackUrlException.class)
                .hasMessageContaining("reserved or internal network");

        verify(clientPersistenceAdapter, never()).upsert(any(), any());
    }

    @Test
    void execute_whenLocalCallbackAndAllowed_registersClient() {
        var useCase = new RegisterClientUseCase(clientPersistenceAdapter, togglePersistenceAdapter, CLOCK, true);
        var command = commandWithCallback("http://localhost:8081/callback");
        var savedInstance = new ClientInstance(
                "01INSTANCE00000000000000000",
                "checkout-service",
                "checkout-7d8d4c7f6f-abcde",
                "checkout-7d8d4c7f6f-abcde",
                "payments",
                "http://localhost:8081/callback",
                ClientInstanceStatus.ACTIVE,
                Instant.now(CLOCK));

        when(togglePersistenceAdapter.findAllByNames(List.of("novo-checkout")))
                .thenReturn(List.of(aToggle("novo-checkout")));
        when(clientPersistenceAdapter.upsert(any(), any())).thenReturn(savedInstance);

        useCase.execute(command);

        verify(clientPersistenceAdapter).upsert(any(), any());
    }

    @Test
    void execute_whenCallbackBlank_throwsInvalidCallbackUrl() {
        var useCase = new RegisterClientUseCase(clientPersistenceAdapter, togglePersistenceAdapter, CLOCK, true);
        var command = commandWithCallback("   ");

        when(togglePersistenceAdapter.findAllByNames(List.of("novo-checkout")))
                .thenReturn(List.of(aToggle("novo-checkout")));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidCallbackUrlException.class)
                .hasMessageContaining("cannot be null or blank");

        verify(clientPersistenceAdapter, never()).upsert(any(), any());
    }

    @Test
    void execute_whenIpv6LoopbackAndNotAllowed_throwsInvalidCallbackUrl() {
        var useCase = new RegisterClientUseCase(clientPersistenceAdapter, togglePersistenceAdapter, CLOCK, false);
        var command = commandWithCallback("http://[::1]:8081/callback");

        when(togglePersistenceAdapter.findAllByNames(List.of("novo-checkout")))
                .thenReturn(List.of(aToggle("novo-checkout")));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidCallbackUrlException.class)
                .hasMessageContaining("reserved or internal network");

        verify(clientPersistenceAdapter, never()).upsert(any(), any());
    }

    @Test
    void execute_whenPrivateCgnatRangeAndNotAllowed_throwsInvalidCallbackUrl() {
        var useCase = new RegisterClientUseCase(clientPersistenceAdapter, togglePersistenceAdapter, CLOCK, false);
        var command = commandWithCallback("http://100.64.10.10:8081/callback");

        when(togglePersistenceAdapter.findAllByNames(List.of("novo-checkout")))
                .thenReturn(List.of(aToggle("novo-checkout")));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidCallbackUrlException.class)
                .hasMessageContaining("reserved or internal network");

        verify(clientPersistenceAdapter, never()).upsert(any(), any());
    }

    @Test
    void execute_whenCallbackUsesInvalidScheme_throwsInvalidCallbackUrl() {
        var useCase = new RegisterClientUseCase(clientPersistenceAdapter, togglePersistenceAdapter, CLOCK, true);
        var command = commandWithCallback("ftp://example.com/callback");

        when(togglePersistenceAdapter.findAllByNames(List.of("novo-checkout")))
                .thenReturn(List.of(aToggle("novo-checkout")));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidCallbackUrlException.class)
                .hasMessageContaining("must use http or https");

        verify(clientPersistenceAdapter, never()).upsert(any(), any());
    }

    @Test
    void execute_whenCallbackContainsUserInfo_throwsInvalidCallbackUrl() {
        var useCase = new RegisterClientUseCase(clientPersistenceAdapter, togglePersistenceAdapter, CLOCK, true);
        var command = commandWithCallback("http://user:pass@example.com/callback");

        when(togglePersistenceAdapter.findAllByNames(List.of("novo-checkout")))
                .thenReturn(List.of(aToggle("novo-checkout")));

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InvalidCallbackUrlException.class)
                .hasMessageContaining("must not contain user info");

        verify(clientPersistenceAdapter, never()).upsert(any(), any());
    }

    private RegisterClientCommand commandWithCallback(String callbackUrl) {
        return new RegisterClientCommand(
                "checkout-service",
                "checkout-7d8d4c7f6f-abcde",
                "checkout-7d8d4c7f6f-abcde",
                "payments",
                callbackUrl,
                List.of(new RegisterClientCommand.SubscriptionCommand("novo-checkout", "LOCAL_CACHE")));
    }

    private Toggle aToggle(String name) {
        return new Toggle(
                "01TOGGLE0000000000000000000",
                name,
                "checkout-service",
                true,
                1L,
                Instant.now(CLOCK),
                null);
    }
}
