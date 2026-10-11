package io.teampulse.identity.application.service.user;

import io.teampulse.common.context.TenantContext;
import io.teampulse.common.reference.ReferenceFactory;
import io.teampulse.identity.api.lifecycle.UserLifecycle;
import io.teampulse.identity.api.lifecycle.UserLifecycleErrorCode;
import io.teampulse.identity.api.lifecycle.UserLifecycleException;
import io.teampulse.identity.api.lifecycle.UserLifecycleResult;
import io.teampulse.identity.api.lifecycle.UserProvisioningCommand;
import io.teampulse.identity.application.port.out.user.IdentityEventPublisher;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.error.UserErrorCode;
import io.teampulse.identity.domain.user.error.UserException;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.events.UserInvited;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Service
@Validated
@Transactional(readOnly = true)
@AllArgsConstructor
public class UserLifecycleService implements UserLifecycle {

    private static final String USER_REFERENCE_PREFIX = "USR";

    private final UserRepository userRepository;
    private final ReferenceFactory referenceFactory;
    private final IdentityEventPublisher identityEventPublisher;

    @Override
    @Transactional
    public UserLifecycleResult create(
        TenantContext tenantContext,
        UserProvisioningCommand command
    ) {
        return mapPublicFailure(() -> toLifecycleResult(persistIfEmailAvailable(
            User.create(
                referenceFactory.generate(USER_REFERENCE_PREFIX),
                tenantContext.tenantReference(),
                command.email(),
                command.firstName(),
                command.lastName()
            )
        )));
    }

    @Override
    @Transactional
    public UserLifecycleResult invite(
        TenantContext tenantContext,
        UserProvisioningCommand command
    ) {
        return mapPublicFailure(() -> {
            User invitedUser = persistIfEmailAvailable(User.invite(
                referenceFactory.generate(USER_REFERENCE_PREFIX),
                tenantContext.tenantReference(),
                command.email(),
                command.firstName(),
                command.lastName()
            ));
            identityEventPublisher.publish(new UserInvited(
                invitedUser.getOrganizationReference(),
                invitedUser.getReference()
            ));
            return toLifecycleResult(invitedUser);
        });
    }

    @Override
    @Transactional
    public void suspend(TenantContext tenantContext, String userReference) {
        mapPublicFailure(() -> updateUser(tenantContext.tenantReference(), userReference, User::suspend));
    }

    @Override
    @Transactional
    public void deactivate(TenantContext tenantContext, String userReference) {
        mapPublicFailure(() -> updateUser(tenantContext.tenantReference(), userReference, User::deactivate));
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

    private static UserLifecycleResult toLifecycleResult(User user) {
        return new UserLifecycleResult(
            user.getReference(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getStatus().name()
        );
    }

    private User updateUser(
        String tenantReference,
        String userReference,
        Consumer<User> transition
    ) {
        User user = userRepository.findByReference(tenantReference, userReference)
            .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND, "User was not found in this organization"));

        transition.accept(user);
        return userRepository.update(user);
    }

    private <T> T mapPublicFailure(Supplier<T> command) {
        try {
            return command.get();
        } catch (UserLifecycleException exception) {
            throw exception;
        } catch (UserException exception) {
            throw toPublicFailure(exception);
        } catch (RuntimeException exception) {
            log.error("User lifecycle operation failed", exception);
            throw new UserLifecycleException(UserLifecycleErrorCode.OPERATION_FAILED, exception);
        }
    }

    private static UserLifecycleException toPublicFailure(UserException exception) {
        UserLifecycleErrorCode userLifecycleErrorCode = switch (exception.getErrorCode()) {
            case NOT_FOUND -> UserLifecycleErrorCode.USER_NOT_FOUND;
            case INVALID_STATUS_TRANSITION -> UserLifecycleErrorCode.TRANSITION_NOT_ALLOWED;
            case EMAIL_ALREADY_USED -> UserLifecycleErrorCode.EMAIL_ALREADY_USED;
            case INVALID_EMAIL, INVALID_FIRST_NAME, INVALID_LAST_NAME ->
                UserLifecycleErrorCode.INVALID_USER_DATA;
            case CONCURRENT_MODIFICATION -> {
                log.warn("User lifecycle operation conflicted with a concurrent update", exception);
                yield UserLifecycleErrorCode.CONCURRENT_MODIFICATION;
            }
            case REFERENCE_GENERATION_FAILED -> {
                log.error("User lifecycle operation failed", exception);
                yield UserLifecycleErrorCode.OPERATION_FAILED;
            }
        };

        return new UserLifecycleException(userLifecycleErrorCode, exception);
    }
}
