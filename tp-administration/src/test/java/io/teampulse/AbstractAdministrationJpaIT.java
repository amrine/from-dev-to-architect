package io.teampulse;

import io.teampulse.testsupport.AbstractJpaIT;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AdministrationTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class AbstractAdministrationJpaIT extends AbstractJpaIT {}
