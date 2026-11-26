package io.teampulse;

import io.teampulse.testsupport.AbstractMvcIT;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AdministrationTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class AbstractAdministrationMvcIT extends AbstractMvcIT {}
