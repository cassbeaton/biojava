package org.biojava.nbio.survival.data;
import org.junit.Before;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CompactCharSequenceTest {
    static CompactCharSequence ccs;

    @Before
    public void setup(){
        ccs = new CompactCharSequence("12345"); //
    }

    @Test
    public void testToString(){
        assertEquals("12345",ccs.toString());
    }

}