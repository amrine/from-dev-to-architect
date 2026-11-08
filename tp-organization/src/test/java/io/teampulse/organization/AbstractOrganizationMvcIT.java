package io.teampulse.organization;

import io.teampulse.testsupport.AbstractMvcIT;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = OrganizationTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "springdoc.api-docs.enabled=true")
public abstract class AbstractOrganizationMvcIT extends AbstractMvcIT {}
