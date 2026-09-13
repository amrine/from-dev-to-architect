@ApplicationModule(
    displayName = "tp-identity",
    allowedDependencies = {
        "common::context",
        "common::mapping",
        "common::persistence",
        "common::reference"
    }
)
package io.teampulse.identity;

import org.springframework.modulith.ApplicationModule;
