package com.graphhopper.traffic.demo;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;
import com.graphhopper.http.GHServer;
import com.graphhopper.http.GHServletModule;
import com.graphhopper.util.CmdArgs;
import com.graphhopper.util.Helper;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoServer {

    public static void main(String[] strArgs) throws Exception {
        CmdArgs args = CmdArgs.readFromConfig("config.properties", "graphhopper.config");
        // command line overwrite configs of properties file
        args.merge(CmdArgs.read(strArgs));
        args.put("osmreader.osm", args.get("datasource", ""));
        args.put("graph.location", args.get("graph.location", "./graph-cache"));
        System.out.println(args.toString());
        new DemoServer(args).start();
    }
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CmdArgs cmdArgs;
    private GHServer server;

    public DemoServer(CmdArgs args) {
        this.cmdArgs = args;
    }

    /**
     * Starts 'pure' GraphHopper server with the specified injector
     */
    public void start(Injector injector) throws Exception {
        server = new GHServer(cmdArgs);
        server.start(injector);
        logger.info("Memory utilization:" + Helper.getMemInfo() + ", " + cmdArgs.get("graph.flagEncoders", ""));
    }

    /**
     * Starts GraphHopper server with routing and matrix module features.
     */
    public void start() throws Exception {
        Injector injector = Guice.createInjector(createModule());
        start(injector);
    }

    protected Module createModule() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                binder().requireExplicitBindings();

                install(new CustomGuiceModule(cmdArgs));
                install(new GHServletModule(cmdArgs) {

                    @Override
                    protected void configureServlets() {
                        super.configureServlets();

                        filter("/*").through(ErrorFilter.class, params);
                        bind(ErrorFilter.class).in(Singleton.class);

                        serve("/datafeed*").with(DataFeedServlet.class);
                        bind(DataFeedServlet.class).in(Singleton.class);

                        serve("/roads*").with(RoadsServlet.class);
                        bind(RoadsServlet.class).in(Singleton.class);
                    }
                });

                bind(GuiceFilter.class);
            }
        };
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
