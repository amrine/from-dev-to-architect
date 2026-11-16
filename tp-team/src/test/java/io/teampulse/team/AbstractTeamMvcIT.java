package io.teampulse.team;

import io.teampulse.testsupport.AbstractMvcIT;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TeamTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class AbstractTeamMvcIT extends AbstractMvcIT {}
