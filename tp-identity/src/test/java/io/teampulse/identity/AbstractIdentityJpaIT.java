package io.teampulse.identity;

import io.teampulse.testsupport.AbstractJpaIT;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = IdentityTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractIdentityJpaIT extends AbstractJpaIT {}
