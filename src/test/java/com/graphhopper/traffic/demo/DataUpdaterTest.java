package com.graphhopper.traffic.demo;

import com.graphhopper.util.Helper;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich
 */
public class DataUpdaterTest {

    @Test
    public void testFeed() throws IOException {
        DataUpdater instance = new DataUpdater(null, null) {
            @Override
            protected String fetchJSONString(String url) throws IOException {
                return Helper.isToString(getClass().getResourceAsStream("example.json"));
            }
        };

        RoadData data = instance.fetch("http://blup.com/somewhere.json");
        assertEquals(3, data.size());

        assertEquals(45, data.get(0).getValue(), 1);
        
        assertEquals("ST028_0", data.get(1).getId());
        assertEquals(20, data.get(1).getValue(), 1);

        assertEquals("ST065_0", data.get(2).getId());
        assertEquals(5, data.get(2).getValue(), 1);
    }
}
