package io.teampulse.identity.application.port.out.user;

import io.teampulse.identity.domain.user.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User create(User user);

    User update(User user);

    Optional<User> findByReference(String organizationRef, String userReference);

    List<User> findAll(String organizationRef);

    boolean existsByEmail(String organizationRef, String canonicalEmail);
}
