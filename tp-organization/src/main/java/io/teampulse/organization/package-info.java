@ApplicationModule(
    displayName = "tp-organization",
    allowedDependencies = {
        "common::mapping",
        "common::reference",
        "identity::user"
    }
)
package io.teampulse.organization;

import org.springframework.modulith.ApplicationModule;
