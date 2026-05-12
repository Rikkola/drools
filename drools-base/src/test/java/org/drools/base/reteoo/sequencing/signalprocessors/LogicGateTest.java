package org.drools.base.reteoo.sequencing.signalprocessors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

public class LogicGateTest {

    @Test
    public void vacuousMatchConstructorRejectsZeroInput() {
        assertThatThrownBy(() ->
                new LogicGate(Gates::nor, 0,
                              new int[0], new int[0],
                              0,
                              true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacuousMatch");
    }

    @Test
    public void vacuousMatchConstructorAcceptsOneFilter() {
        assertThatCode(() ->
                new LogicGate(Gates::nor, 0,
                              new int[]{0}, new int[]{0},
                              0,
                              true))
                .doesNotThrowAnyException();
    }

    @Test
    public void existingFiveArgConstructorStillWorks() {
        assertThatCode(() ->
                new LogicGate(Gates::and, 0,
                              new int[]{0}, new int[]{0},
                              0))
                .doesNotThrowAnyException();
    }
}
