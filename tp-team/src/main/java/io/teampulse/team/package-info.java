@ApplicationModule(
    displayName = "tp-team",
    allowedDependencies = {
        "common::context",
        "common::mapping",
        "common::reference",
        "identity::user",
        "organization::organization"
    }
)
package io.teampulse.team;

import org.springframework.modulith.ApplicationModule;
