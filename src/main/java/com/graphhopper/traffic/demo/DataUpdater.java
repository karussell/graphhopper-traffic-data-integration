package com.graphhopper.traffic.demo;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import gnu.trove.set.hash.TIntHashSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Peter Karich
 */
public class DataUpdater {

    private final OkHttpClient client = new OkHttpClient();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final GraphHopper hopper;
    private final Lock writeLock;
    private final long seconds = 150;
    private RoadData currentRoads;

    public DataUpdater(GraphHopper hopper, Lock writeLock) {
        this.hopper = hopper;
        this.writeLock = writeLock;
        client.setConnectTimeout(5, TimeUnit.SECONDS);
    }

    public void feed(RoadData data) {
        writeLock.lock();
        try {
            lockedFeed(data);
        } finally {
            writeLock.unlock();
        }
    }

    private void lockedFeed(RoadData data) {
        currentRoads = data;
        Graph graph = hopper.getGraph();
        FlagEncoder carEncoder = hopper.getEncodingManager().getEncoder("car");
        LocationIndex locationIndex = hopper.getLocationIndex();

        int errors = 0;
        int updates = 0;
        TIntHashSet edgeIds = new TIntHashSet(data.size());
        for (RoadEntry entry : data) {

            // TODO get more than one point -> our map matching component
            Point point = entry.getPoints().get(entry.getPoints().size() / 2);
            QueryResult qr = locationIndex.findClosest(point.lat, point.lon, EdgeFilter.ALL_EDGES);
            if (!qr.isValid()) {
                // logger.info("no matching road found for entry " + entry.getId() + " at " + point);
                errors++;
                continue;
            }

            int edgeId = qr.getClosestEdge().getEdge();
            if (edgeIds.contains(edgeId)) {
                // TODO this wouldn't happen with our map matching component
                errors++;
                continue;
            }

            edgeIds.add(edgeId);
            EdgeIteratorState edge = graph.getEdgeProps(edgeId, Integer.MIN_VALUE);
            double value = entry.getValue();
            if ("replace".equalsIgnoreCase(entry.getMode())) {
                if ("speed".equalsIgnoreCase(entry.getValueType())) {
                    double oldSpeed = carEncoder.getSpeed(edge.getFlags());
                    if (oldSpeed != value) {
                        updates++;
                        // TODO use different speed for the different directions (see e.g. Bike2WeightFlagEncoder)
                        logger.info("Speed change at " + entry.getId() + " (" + point + "). Old: " + oldSpeed + ", new:" + value);
                        edge.setFlags(carEncoder.setSpeed(edge.getFlags(), value));
                    }
                } else {
                    throw new IllegalStateException("currently no other value type than 'speed' is supported");
                }
            } else {
                throw new IllegalStateException("currently no other mode than 'replace' is supported");
            }
        }

        logger.info("Updated " + updates + " street elements of " + data.size() + ". Unchanged:" + (data.size() - updates) + ", errors:" + errors);
    }

    protected String fetchJSONString(String url) throws IOException {
        Request okRequest = new Request.Builder().url(url).build();
        return client.newCall(okRequest).execute().body().string();
    }

    public RoadData fetch(String url) throws IOException {
        JSONObject json = new JSONObject(fetchJSONString(url));
        JSONArray arr = json.getJSONArray("features");
        RoadData data = new RoadData();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            JSONObject attr = obj.getJSONObject("attributes");
            int streetUsage = attr.getInt("AUSLASTUNG");
            String idStr = attr.getString("IDENTIFIER");

            // according to the docs http://www.offenedaten-koeln.de/dataset/verkehrskalender-der-stadt-k%C3%B6ln
            // there are only three indications 0='ok', 1='slow' and 2='traffic jam'
            if (streetUsage == 0 || streetUsage == 1 || streetUsage == 2) {
                double speed;
                if (streetUsage == 1) {
                    speed = 20;
                } else if (streetUsage == 2) {
                    speed = 5;
                } else {
                    // if there is a traffic jam we need to revert afterwards!
                    speed = 45; // TODO getOldSpeed();
                }
                JSONArray paths = obj.getJSONObject("geometry").getJSONArray("paths");
                for (int pathPointIndex = 0; pathPointIndex < paths.length(); pathPointIndex++) {
                    List<Point> points = new ArrayList<Point>();
                    JSONArray pathPoints = paths.getJSONArray(pathPointIndex);
                    for (int pointIndex = 0; pointIndex < pathPoints.length(); pointIndex++) {
                        JSONArray point = pathPoints.getJSONArray(pointIndex);
                        points.add(new Point(point.getDouble(1), point.getDouble(0)));
                    }

                    if (!points.isEmpty()) {
                        data.add(new RoadEntry(idStr + "_" + pathPointIndex, points, speed, "speed", "replace"));
                    }
                }
            }
        }

        return data;
    }

    private final AtomicBoolean running = new AtomicBoolean(false);

    public void start() {
        if (running.get()) {
            return;
        }

        running.set(true);
        new Thread("DataUpdater" + seconds) {
            @Override
            public void run() {
                logger.info("fetch new data every " + seconds + " seconds");
                while (running.get()) {
                    try {
                        logger.info("fetch new data");
                        RoadData data = fetch("http://www.stadt-koeln.de/externe-dienste/open-data/traffic.php");
                        feed(data);
                        try {
                            Thread.sleep(seconds * 1000);
                        } catch (InterruptedException ex) {
                            logger.info("update thread stopped");
                            break;
                        }
                    } catch (Exception ex) {
                        logger.error("Problem while fetching data", ex);
                    }
                }
            }
        }.start();
    }

    public void stop() {
        running.set(false);
    }

    public RoadData getAll() {
        if (currentRoads == null) {
            return new RoadData();
        }

        return currentRoads;
    }
}
