package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.identity.application.port.in.user.ListUsersUseCase;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class ListUsersService implements ListUsersUseCase {
    private final UserRepository userRepository;

    @Override
    public List<User> list(TenantContext tenantContext) {
        return userRepository.findAll(tenantContext.tenantReference());
    }
}
