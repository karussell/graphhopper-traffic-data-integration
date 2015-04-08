package com.graphhopper.traffic.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.graphhopper.http.GraphHopperServlet;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Peter Karich
 */
public class RoadsServlet extends GraphHopperServlet {

    @Inject
    private ObjectMapper mapper;

    @Inject
    private DataUpdater updater;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        mapper.writeValue(res.getOutputStream(), updater.getAll());
    }
}
