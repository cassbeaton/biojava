package org.biojava.nbio.survival.kaplanmeier.figure;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import java.util.Optional;

public class CensorStatusTest {
    static CensorStatus cs;

    @Before
    public void setup(){
        cs = new CensorStatus("GroupA",100.00,"UNIT_TEST");
    }

    @Test
    public void testCompareToMore() {
        CensorStatus csToCompareMore = new CensorStatus("GroupA",150.00,"UNIT_TEST");

        assertEquals(-1, cs.compareTo(csToCompareMore));
    }

    @Test
    public void testCompareToLess() {
        CensorStatus csToCompareLess = new CensorStatus("GroupA",100.00,"DIFFERENT");

        assertEquals(1, cs.compareTo(csToCompareLess));
    }

    @Test
    public void testCompareToSame() {
        CensorStatus csToCompareSame = new CensorStatus("GroupA",100.0,"UNIT_TEST");

        assertEquals(0, cs.compareTo(csToCompareSame));
    }

    @Test
    public void testGetCopy() {
        CensorStatus csCopy = cs.getCopy();
        assertEquals(null, csCopy.row);
        assertEquals(Optional.of(100.0), Optional.ofNullable(csCopy.time));
        assertEquals("UNIT_TEST", csCopy.censored);
        assertEquals("GroupA", csCopy.group);
        assertEquals(null, csCopy.value);
        assertEquals(null, csCopy.zscore);
    }

}
















