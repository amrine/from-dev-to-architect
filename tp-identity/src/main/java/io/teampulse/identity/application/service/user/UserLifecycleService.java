package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.application.port.in.user.CreateUserCommand;
import io.teampulse.identity.application.port.in.user.UserLifecycleUseCase;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.events.UserInvited;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.function.Consumer;

@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class UserLifecycleService implements UserLifecycleUseCase {
    private static final String USER_REFERENCE_PREFIX = "USR";

    private final UserRepository userRepository;
    private final ReferenceFactory referenceFactory;
    private final ApplicationEventPublisher eventPublisher;

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

        return persistIfEmailAvailable(user);
    }

    @Override
    @Transactional
    public User invite(TenantContext tenantContext, CreateUserCommand command) {
        String organizationReference = tenantContext.tenantReference();
        String userReference = referenceFactory.generate(USER_REFERENCE_PREFIX);

        User user = User.invite(
            userReference,
            organizationReference,
            command.email(),
            command.firstName(),
            command.lastName()
        );
        User invitedUser = persistIfEmailAvailable(user);
        eventPublisher.publishEvent(new UserInvited(
            invitedUser.getOrganizationReference(),
            invitedUser.getReference()
        ));
        return invitedUser;
    }

    @Override
    @Transactional
    public User suspend(TenantContext tenantContext, String userReference) {
        return updateUser(tenantContext, userReference, User::suspend);
    }

    @Override
    @Transactional
    public User deactivate(TenantContext tenantContext, String userReference) {
        return updateUser(tenantContext, userReference, User::deactivate);
    }

    private User persistIfEmailAvailable(User user) {
        String organizationReference = user.getOrganizationReference();
        if (userRepository.existsByEmail(organizationReference, user.getEmail())) {
            throw new UserException(
                UserErrorCode.EMAIL_ALREADY_USED,
                "Email is already used in this organization"
            );
        }

        return userRepository.create(user);
    }

    private User updateUser(
        TenantContext tenantContext,
        String userReference,
        Consumer<User> transition
    ) {
        String organizationReference = tenantContext.tenantReference();
        User user = userRepository.findByReference(organizationReference, userReference)
            .orElseThrow(() -> new UserException(
                UserErrorCode.NOT_FOUND,
                "User was not found in this organization"
            ));

        transition.accept(user);
        return userRepository.update(user);
    }
}
