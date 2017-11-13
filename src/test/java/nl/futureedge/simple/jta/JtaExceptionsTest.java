package nl.futureedge.simple.jta;

import org.junit.Test;

public class JtaExceptionsTest {

    @Test
    public void constructor() throws Exception {
        ReflectionTestUtils.testNotInstantiable(JtaExceptions.class);
    }
}
