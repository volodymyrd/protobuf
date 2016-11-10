package com.gmail.volodymyrdotsenko.grpc.examples.routeguide;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * A sample gRPC server that serve the RouteGuide (see route_guide.proto) service.
 */
public class RouteGuideServer {

    private static final Logger logger = Logger.getLogger(RouteGuideServer.class.getName());

    public static void main(String[] args) throws Exception {
        RouteGuideServer server = new RouteGuideServer(8980);
        server.start();
        server.blockUntilShutdown();
    }

    private final int port;
    private final Server server;

    public RouteGuideServer(int port) throws IOException {
        this(port, RouteGuideUtil.getDefaultFeaturesFile().get());
    }

    /**
     * Create a RouteGuide server listening on {@code port} using {@code featureFile} database.
     */
    public RouteGuideServer(int port, URL featureFile) throws IOException {
        this(ServerBuilder.forPort(port), port, RouteGuideUtil.parseFeatures(featureFile));
    }

    /**
     * Create a RouteGuide server using serverBuilder as a base and features as data.
     */
    public RouteGuideServer(ServerBuilder<?> serverBuilder, int port, Collection<Feature> features) {
        this.port = port;
        server = serverBuilder.addService(new RouteGuideService(features))
            .build();
    }

    /**
     * Start serving requests.
     */
    private void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                RouteGuideServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Our implementation of RouteGuide service.
     *
     * <p>See route_guide.proto for details of the methods.
     */
    private static class RouteGuideService extends RouteGuideGrpc.RouteGuideImplBase {

        private final Collection<Feature> features;

        RouteGuideService(Collection<Feature> features) {
            this.features = features;
        }

        /**
         * Gets the {@link Feature} at the requested {@link Point}. If no feature at that location
         * exists, an unnamed feature is returned at the provided location.
         *
         * @param request          the requested location for the feature.
         * @param responseObserver the observer that will receive the feature at the requested point.
         */
        @Override
        public void getFeature(Point request, StreamObserver<Feature> responseObserver) {
            responseObserver.onNext(checkFeature(request));
            responseObserver.onCompleted();
        }

        /**
         * Gets the feature at the given point.
         *
         * @param location the location to check.
         * @return The feature object at the point. Note that an empty name indicates no feature.
         */
        private Feature checkFeature(Point location) {
            for (Feature feature : features) {
                if (feature.getLocation().getLatitude() == location.getLatitude()
                    && feature.getLocation().getLongitude() == location.getLongitude()) {
                    return feature;
                }
            }

            // No feature was found, return an unnamed feature.
            return Feature.newBuilder().setName("").setLocation(location).build();
        }
    }
}