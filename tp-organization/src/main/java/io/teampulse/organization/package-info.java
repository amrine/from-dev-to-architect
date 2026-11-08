@ApplicationModule(
    displayName = "tp-organization",
    allowedDependencies = {
        "common::error",
        "common::mapping",
        "common::persistence",
        "common::reference",
        "identity::user",
        "websupport"
    }
)
package io.teampulse.organization;

import org.springframework.modulith.ApplicationModule;
