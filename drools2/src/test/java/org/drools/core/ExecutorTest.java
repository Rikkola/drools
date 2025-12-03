package org.drools.core;

import org.junit.jupiter.api.Test;

public class ExecutorTest {

    @Test
    public void test1() {
        Executor exc = new Executor();
        exc.dataStore().add(null, null);
        exc.ruleUnit().start(null);
    }
}
