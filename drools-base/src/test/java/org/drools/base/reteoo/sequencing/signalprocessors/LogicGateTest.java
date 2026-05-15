package org.drools.base.reteoo.sequencing.signalprocessors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

public class LogicGateTest {

    @Test
    public void vacuousMatchAtActivationConstructorRejectsZeroInput() {
        assertThatThrownBy(() ->
                new LogicGate(
                        Gates::and,
                        0,
                        new int[0],
                        new int[0],
                        0,
                        true,    // vacuousMatchAtActivation
                        true))   // statusCanRevert
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacuousMatchAtActivation");
    }

    @Test
    public void vacuousMatchAtActivationConstructorAcceptsOneFilter() {
        assertThatCode(() ->
                new LogicGate(Gates::nor, 0,
                              new int[]{0}, new int[]{0},
                              0,
                              true,    // vacuousMatchAtActivation
                              true))   // statusCanRevert
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
