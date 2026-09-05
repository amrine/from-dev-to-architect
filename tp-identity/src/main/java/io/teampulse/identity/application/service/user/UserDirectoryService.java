package io.teampulse.identity.application.service.user;

import io.teampulse.identity.api.user.UserAvailability;
import io.teampulse.identity.api.user.UserDirectory;
import io.teampulse.identity.api.user.UserDirectoryException;
import io.teampulse.identity.application.port.out.user.UserRepository;
import io.teampulse.identity.domain.user.model.User;
import io.teampulse.identity.domain.user.model.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@Validated
@Transactional(readOnly = true)
public class UserDirectoryService implements UserDirectory {

    private final UserRepository userRepository;

    public UserDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserAvailability check(
        String organizationReference,
        String userReference
    ) {
        Optional<User> user;
        try {
            user = userRepository.findByReference(
                organizationReference,
                userReference
            );
        } catch (RuntimeException exception) {
            throw new UserDirectoryException(exception);
        }

        return user
            .map(foundUser -> toAvailability(foundUser.getStatus()))
            .orElse(UserAvailability.NOT_FOUND);
    }

    private UserAvailability toAvailability(UserStatus status) {
        return switch (status) {
            case ACTIVE -> UserAvailability.AVAILABLE;
            case INVITED, CREATING -> UserAvailability.PENDING;
            case SUSPENDED, DEACTIVATED -> UserAvailability.UNAVAILABLE;
        };
    }
}
