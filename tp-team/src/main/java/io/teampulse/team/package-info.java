@ApplicationModule(
        displayName = "tp-team",
        allowedDependencies = {
            "common::context",
            "common::error",
            "common::mapping",
            "common::persistence",
            "common::reference",
            "identity::user",
            "organization::organization",
            "websupport"
        })
package io.teampulse.team;

import org.springframework.modulith.ApplicationModule;
