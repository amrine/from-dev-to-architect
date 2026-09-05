package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockMakers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersServiceTest {

    private static final String ORGANIZATION_REFERENCE =
        "ORG-2026-3008-00000ZA7B900";

    @Mock(mockMaker = MockMakers.PROXY)
    private UserRepository userRepository;

    private ListUsersService service;

    @BeforeEach
    void setUp() {
        service = new ListUsersService(userRepository);
    }

    @Test
    void callsFindAllWithTenantReference() {
        List<User> users = List.of();
        when(userRepository.findAll(ORGANIZATION_REFERENCE)).thenReturn(users);

        service.list(validTenantContext());

        verify(userRepository).findAll(ORGANIZATION_REFERENCE);
    }

    @Test
    void returnsUsersProvidedByRepository() {
        List<User> users = List.of(
            createUser("USR-2026-3008-00000ZA7B900", "alice@example.com"),
            createUser("USR-2026-3008-00000ZA7B901", "bob@example.com")
        );
        when(userRepository.findAll(ORGANIZATION_REFERENCE)).thenReturn(users);

        List<User> result = service.list(validTenantContext());

        assertSame(users, result);
    }

    @Test
    void returnsEmptyListProvidedByRepository() {
        List<User> users = List.of();
        when(userRepository.findAll(ORGANIZATION_REFERENCE)).thenReturn(users);

        List<User> result = service.list(validTenantContext());

        assertSame(users, result);
    }

    private static TenantContext validTenantContext() {
        return new TenantContext(ORGANIZATION_REFERENCE);
    }

    private static User createUser(String reference, String email) {
        return User.create(
            reference,
            ORGANIZATION_REFERENCE,
            email,
            "First name",
            "Last name"
        );
    }
}
