package io.teampulse.identity.infrastructure.web.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.context.TenantContextProvider;
import io.teampulse.identity.application.port.in.user.CreateUserCommand;
import io.teampulse.identity.application.port.in.user.ListUsersUseCase;
import io.teampulse.identity.application.port.in.user.UserLifecycleUseCase;
import io.teampulse.identity.domain.user.model.User;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private final UserLifecycleUseCase userLifecycleUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final TenantContextProvider tenantContextProvider;

    @PostMapping
    public ResponseEntity<UserResponse> create(
        @Valid @RequestBody CreateUserRequest request
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        User user = userLifecycleUseCase.create(tenantContext, command(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response(user));
    }

    @PostMapping("/invitations")
    public ResponseEntity<UserResponse> invite(
        @Valid @RequestBody CreateUserRequest request
    ) {
        TenantContext tenantContext = tenantContextProvider.current();
        User user = userLifecycleUseCase.invite(tenantContext, command(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response(user));
    }

    @GetMapping
    public List<UserResponse> list() {
        TenantContext tenantContext = tenantContextProvider.current();
        return listUsersUseCase.list(tenantContext).stream()
            .map(UserController::response)
            .toList();
    }

    private static CreateUserCommand command(CreateUserRequest request) {
        return new CreateUserCommand(
            request.email(),
            request.firstName(),
            request.lastName()
        );
    }

    private static UserResponse response(User user) {
        return new UserResponse(
            user.getReference(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name()
        );
    }
}
