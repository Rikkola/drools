package org.drools.base.reteoo.sequencing.signalprocessors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GatesTest {

    // NOR: true when no expected bit is currently matched.
    // a = currentMatched, b = allMatched (sourceMask).

    @Test
    public void norNoneMatched() {
        // 2 expected inputs, none fired
        assertThat(Gates.nor(0b00L, 0b11L)).isTrue();
    }

    @Test
    public void norOneMatched() {
        // 2 expected inputs, only first fired
        assertThat(Gates.nor(0b01L, 0b11L)).isFalse();
    }

    @Test
    public void norAllMatched() {
        // 2 expected inputs, both fired
        assertThat(Gates.nor(0b11L, 0b11L)).isFalse();
    }

    // XOR: true when exactly one expected bit is currently matched.

    @Test
    public void xorNoneMatched() {
        assertThat(Gates.xor(0b00L, 0b11L)).isFalse();
    }

    @Test
    public void xorOneMatchedLow() {
        assertThat(Gates.xor(0b01L, 0b11L)).isTrue();
    }

    @Test
    public void xorOneMatchedHigh() {
        assertThat(Gates.xor(0b10L, 0b11L)).isTrue();
    }

    @Test
    public void xorAllMatched() {
        assertThat(Gates.xor(0b11L, 0b11L)).isFalse();
    }
}
