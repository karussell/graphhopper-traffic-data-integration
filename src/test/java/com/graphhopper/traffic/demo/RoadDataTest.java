package com.graphhopper.traffic.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Peter Karich
 */
public class RoadDataTest {

    private final ObjectMapper mapper = CustomGuiceModule.createMapper();

    @Test
    public void testReadFromFile() throws IOException {
        RoadData data = mapper.readValue(new StringReader("[{'id':'1', 'points': [[11.1, 42.4]], 'value': 20.5, 'value_type': 'speed', 'mode':'replace'}]".replaceAll("'", "\"")), RoadData.class);
        RoadEntry entry = data.get(0);
        assertEquals(1, entry.getPoints().size());
        assertEquals(42.4, entry.getPoints().get(0).lat, 0.01);
        assertEquals(11.1, entry.getPoints().get(0).lon, 0.01);
        assertEquals(20.5, entry.getValue(), 0.1);
        assertEquals("speed", entry.getValueType());
    }

    @Test
    public void testWriteToFile() throws IOException {
        RoadData data = new RoadData();
        data.add(new RoadEntry("1", Arrays.asList(new Point(42.4, 11.1)), 2, "speed", "replace"));
        StringWriter sWriter = new StringWriter();
        mapper.writeValue(sWriter, data);
        assertEquals("[{\"points\":[[11.1,42.4]],\"value\":2.0,\"value_type\":\"speed\",\"mode\":\"replace\",\"id\":\"1\"}]", sWriter.toString());
    }
}
