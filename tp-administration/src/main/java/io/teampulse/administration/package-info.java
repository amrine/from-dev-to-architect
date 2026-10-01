@ApplicationModule(
    displayName = "tp-administration",
    allowedDependencies = {
        "common::context",
        "common::error",
        "identity::lifecycle",
        "organization::organization",
        "team::api"
    }
)
package io.teampulse.administration;

import org.springframework.modulith.ApplicationModule;
