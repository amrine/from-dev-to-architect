package io.teampulse.team;

import io.teampulse.testsupport.AbstractMvcIT;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TeamTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractTeamIT extends AbstractMvcIT {}
