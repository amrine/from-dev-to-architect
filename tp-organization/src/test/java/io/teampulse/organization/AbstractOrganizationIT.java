package io.teampulse.organization;

import io.teampulse.testsupport.AbstractMvcIT;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = OrganizationTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractOrganizationIT extends AbstractMvcIT {}
