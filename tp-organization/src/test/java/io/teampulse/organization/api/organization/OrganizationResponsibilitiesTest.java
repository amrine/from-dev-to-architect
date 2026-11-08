package io.teampulse.organization.api.organization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationResponsibilitiesTest {

    @Test
    void requiresReferenceAndStatus() {
        assertThrows(NullPointerException.class,
            () -> new OrganizationResponsibilities(null, "CREATING", null, null));
        assertThrows(NullPointerException.class,
            () -> new OrganizationResponsibilities("ORG-2026-1409-00000ZA7B940", null, null, null));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsBlankOrganizationReferences(String reference) {
        assertThrows(IllegalArgumentException.class,
            () -> new OrganizationResponsibilities(reference, "CREATING", null, null));
    }

    @Test
    void keepsUnassignedResponsibilitiesNull() {
        var responsibilities = new OrganizationResponsibilities("ORG-2026-1409-00000ZA7B940", "CREATING", null, null);
        assertEquals("ORG-2026-1409-00000ZA7B940", responsibilities.organizationReference());
        assertEquals("CREATING", responsibilities.status());
        assertNull(responsibilities.administratorReference());
        assertNull(responsibilities.managerReference());
    }
}
