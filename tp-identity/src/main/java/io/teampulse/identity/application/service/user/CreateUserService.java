package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.application.port.in.user.CreateUserCommand;
import io.teampulse.identity.application.port.in.user.CreateUserUseCase;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
public class CreateUserService implements CreateUserUseCase {
    private static final String USER_REFERENCE_PREFIX = "USR";

    private final UserRepository userRepository;
    private final ReferenceFactory referenceFactory;

    public CreateUserService(UserRepository userRepository,
                             ReferenceFactory referenceFactory) {
        this.userRepository = userRepository;
        this.referenceFactory = referenceFactory;
    }

    @Override
    @Transactional
    public User create(TenantContext tenantContext, CreateUserCommand command) {
        String organizationReference = tenantContext.tenantReference();
        String userReference = referenceFactory.generate(USER_REFERENCE_PREFIX);

        User user = User.create(
            userReference,
            organizationReference,
            command.email(),
            command.firstName(),
            command.lastName()
        );

        if (userRepository.existsByEmail(organizationReference, user.getEmail())) {
            throw new UserException(
                UserErrorCode.EMAIL_ALREADY_USED,
                "Email is already used in this organization"
            );
        }

        return userRepository.create(user);
    }
}
