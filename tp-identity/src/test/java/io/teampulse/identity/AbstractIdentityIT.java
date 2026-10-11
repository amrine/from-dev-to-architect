package io.teampulse.identity;

import io.teampulse.testsupport.AbstractMvcIT;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = IdentityTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class AbstractIdentityIT extends AbstractMvcIT {}
