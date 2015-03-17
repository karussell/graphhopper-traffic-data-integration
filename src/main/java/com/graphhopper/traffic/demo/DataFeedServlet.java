package com.graphhopper.traffic.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.graphhopper.GraphHopper;
import com.graphhopper.http.GraphHopperServlet;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Peter Karich
 */
public class DataFeedServlet extends GraphHopperServlet {

    @Inject
    private ObjectMapper mapper;

    @Inject
    private GraphHopper hopper;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            RoadData data = mapper.readValue(req.getInputStream(), RoadData.class);
            System.out.println("data:" + data);

            Graph graph = hopper.getGraph();
            FlagEncoder encoder = hopper.getEncodingManager().getEncoder("car");
            LocationIndex locationIndex = hopper.getLocationIndex();

            // TODO make thread safe and lock routing when we update!
            int errors = 0;
            for (RoadEntry entry : data) {

                // TODO get more than one point
                Point point = entry.getPoints().get(0);
                QueryResult qr = locationIndex.findClosest(point.lat, point.lon, EdgeFilter.ALL_EDGES);
                if (!qr.isValid()) {
                    // logger.info("no matching road found for entry " + entry.getId() + " at " + point);
                    errors++;
                    continue;
                }
                EdgeIteratorState edge = graph.getEdgeProps(qr.getClosestEdge().getEdge(), Integer.MIN_VALUE);
                double value = entry.getValue();
                if ("replace".equalsIgnoreCase(entry.getMode())) {
                    if ("speed".equalsIgnoreCase(entry.getValueType())) {

                        // TODO use different speed for the different directions (see e.g. Bike2WeightFlagEncoder)
                        edge.setFlags(encoder.setSpeed(edge.getFlags(), value));
                    } else {
                        throw new IllegalStateException("currently no other value type than 'speed' is supported");
                    }
                } else {
                    throw new IllegalStateException("currently no other mode than 'replace' is supported");
                }
            }
            logger.info("Updated " + (data.size() - errors) + " street elements. Errors:" + errors);
        } catch (Exception ex) {
            logger.error("Problem while feeding", ex);
        }
    }
}
