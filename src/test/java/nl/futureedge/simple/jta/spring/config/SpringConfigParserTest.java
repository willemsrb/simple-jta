package nl.futureedge.simple.jta.spring.config;

import nl.futureedge.simple.jta.ReflectionTestUtils;
import org.junit.Test;

public class SpringConfigParserTest {

    @Test
    public void constructor() throws Exception {
        ReflectionTestUtils.testNotInstantiable(SpringConfigParser.class);
    }
}
