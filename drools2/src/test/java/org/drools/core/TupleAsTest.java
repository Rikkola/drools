package org.drools.core;

import org.drools.core.function.Tuple.Tuple1;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
import org.drools.core.function.Tuple.Tuple4;
import org.drools.core.function.Tuple.Tuple5;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TupleAsTest {
    public record RSize1(String fld1) {};
    public record RSize2(String fld1, String fld2) {};
    public record RSize3(String fld1, String fld2, String fld3) {};
    public record RSize4(String fld1, String fld2, String fld3, String fld4) {};
    public record RSize5(String fld1, String fld2, String fld3, String fld4, String fld5) {};

    public static class CSize1 {
        protected String fld1;

        public CSize1() {}

        public CSize1(String fld1) {
            this.fld1 = fld1;
        }

        public String getFld1() {
            return fld1;
        }

        public void setFld1(String fld1) {
            this.fld1 = fld1;
        }
    }

    public static class CSize2 extends CSize1 {
        protected String fld2;

        public CSize2() {super();}

        public CSize2(String fld1, String fld2) {
            super(fld1);
            this.fld2 = fld2;
        }

        public String getFld2() {
            return fld2;
        }

        public void setFld2(String fld2) {
            this.fld2 = fld2;
        }
    }

    public static class CSize3 extends CSize2 {
        protected String fld3;

        public CSize3() {super();}


        public CSize3(String fld1, String fld2, String fld3) {
            super(fld1, fld2);
            this.fld3 = fld3;
        }

        public String getFld3() {
            return fld3;
        }

        public void setFld3(String fld3) {
            this.fld3 = fld3;
        }
    }

    public static class CSize4 extends CSize3 {
        protected String fld4;

        public CSize4() {super();}

        public CSize4(String fld1, String fld2, String fld3, String fld4) {
            super(fld1, fld2, fld3);
            this.fld4 = fld4;
        }

        public String getFld4() {
            return fld4;
        }

        public void setFld4(String fld4) {
            this.fld4 = fld4;
        }
    }

    public static class CSize5 extends CSize4 {
        protected String fld5;

        public CSize5() {super();}

        public CSize5(String fld1, String fld2, String fld3, String fld4, String fld5) {
            super(fld1, fld2, fld3, fld4);
            this.fld5 = fld5;
        }

        public String getFld5() {
            return fld5;
        }

        public void setFld5(String fld5) {
            this.fld5 = fld5;
        }
    }

    @Test
    public void testRSize1() {
        Tuple1 t  = new Tuple1("val1");
        RSize1 s1 = t.as();
        assertThat(s1.fld1()).isEqualTo(t.getA());
    }

    @Test
    public void testCSize1() {
        Tuple1 t  = new Tuple1("val1");
        CSize1 s1 = t.as();
        assertThat(s1.getFld1()).isEqualTo(t.getA());
    }


    @Test
    public void testRSize2() {
        Tuple2 t  = new Tuple2("val1", "val2");
        RSize2 s2 = t.as();
        assertThat(s2.fld1()).isEqualTo(t.getA());
        assertThat(s2.fld2()).isEqualTo(t.getB());
    }

    @Test
    public void testCSize2() {
        Tuple2 t  = new Tuple2("val1", "val2");
        CSize2 s2 = t.as();
        assertThat(s2.getFld1()).isEqualTo(t.getA());
        assertThat(s2.getFld2()).isEqualTo(t.getB());
    }

    @Test
    public void testRSize3() {
        Tuple3 t  = new Tuple3("val1", "val2", "val3");
        RSize3 s2 = t.as();
        assertThat(s2.fld1()).isEqualTo(t.getA());
        assertThat(s2.fld2()).isEqualTo(t.getB());
        assertThat(s2.fld3()).isEqualTo(t.getC());
    }

    @Test
    public void testCSize3() {
        Tuple3 t  = new Tuple3("val1", "val2", "val3");
        CSize3 s2 = t.as();
        assertThat(s2.getFld1()).isEqualTo(t.getA());
        assertThat(s2.getFld2()).isEqualTo(t.getB());
        assertThat(s2.getFld3()).isEqualTo(t.getC());
    }

    @Test
    public void testRSize4() {
        Tuple4 t  = new Tuple4("val1", "val2", "val3", "val4");
        RSize4 s2 = t.as();
        assertThat(s2.fld1()).isEqualTo(t.getA());
        assertThat(s2.fld2()).isEqualTo(t.getB());
        assertThat(s2.fld3()).isEqualTo(t.getC());
        assertThat(s2.fld4()).isEqualTo(t.getD());
    }

    @Test
    public void testCSize4() {
        Tuple4 t  = new Tuple4("val1", "val2", "val3", "val4");
        CSize4 s2 = t.as();
        assertThat(s2.getFld1()).isEqualTo(t.getA());
        assertThat(s2.getFld2()).isEqualTo(t.getB());
        assertThat(s2.getFld3()).isEqualTo(t.getC());
        assertThat(s2.getFld4()).isEqualTo(t.getD());
    }

    @Test
    public void testRSize5() {
        Tuple5 t  = new Tuple5("val1", "val2", "val3", "val4","val5");
        RSize5 s2 = t.as();
        assertThat(s2.fld1()).isEqualTo(t.getA());
        assertThat(s2.fld2()).isEqualTo(t.getB());
        assertThat(s2.fld3()).isEqualTo(t.getC());
        assertThat(s2.fld4()).isEqualTo(t.getD());
        assertThat(s2.fld5()).isEqualTo(t.getE());
    }

    @Test
    public void testCSize5() {
        Tuple5 t  = new Tuple5("val1", "val2", "val3", "val4","val5");
        CSize5 s2 = t.as();
        assertThat(s2.getFld1()).isEqualTo(t.getA());
        assertThat(s2.getFld2()).isEqualTo(t.getB());
        assertThat(s2.getFld3()).isEqualTo(t.getC());
        assertThat(s2.getFld4()).isEqualTo(t.getD());
        assertThat(s2.getFld5()).isEqualTo(t.getE());
    }
}
