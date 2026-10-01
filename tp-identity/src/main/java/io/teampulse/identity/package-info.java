@ApplicationModule(
    displayName = "tp-identity",
    allowedDependencies = {
        "common::context",
        "common::error",
        "common::mapping",
        "common::persistence",
        "common::reference",
        "websupport"
    }
)
package io.teampulse.identity;

import org.springframework.modulith.ApplicationModule;
