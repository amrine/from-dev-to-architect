package io.teampulse.common.context;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void rejectsNullEmptyOrBlankTenantReference(String tenantReference) {
        assertThrows(
            IllegalArgumentException.class,
            () -> new TenantContext(tenantReference)
        );
    }
}
