package io.teampulse.organization;

import io.teampulse.testsupport.AbstractJpaIT;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = OrganizationTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractOrganizationJpaIT extends AbstractJpaIT {}
