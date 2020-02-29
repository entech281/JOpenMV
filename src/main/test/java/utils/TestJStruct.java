package utils;

import static org.junit.Test;
import static org.junit.assertEquals;

public class TestJStruct{
    @Test
    public void TestJStructStuff(){
        byte[] expected = new byte[] {(byte)0x06,(byte) 0x87,(byte)0x0b};
        byte[] actual;

        byte a = (byte)0x06;
        byte b = (byte)0x87;
        byte c = (byte)0x0b;

        JStruct struct = new JStruct();

        long[] whatIGot = struct.pack(">BBB", a,b,c);

        assertEquals(whatIGot.length = actual.length);

        for(int i = 0; i < whatIGot.length; ++i)
            assertEquals((byte)whatIGot[i], expected[i]);

    }
}