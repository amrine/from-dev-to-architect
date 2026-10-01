package io.teampulse.team.api;

import io.teampulse.common.context.TenantContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Public tenant-scoped query for teams where a user is an administrator or manager. */
public interface TeamResponsibilityDirectory {

    List<TeamResponsibilitySnapshot> findByResponsibleUser(
        @NotNull TenantContext tenantContext,
        @NotBlank String userReference
    );
}
