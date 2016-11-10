package com.gmail.volodymyrdotsenko.grpc.examples.routeguide;

import io.grpc.ManagedChannel;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.gmail.volodymyrdotsenko.grpc.examples.routeguide.RouteGuideGrpc.*;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * Sample client code that makes gRPC calls to the server.
 */
public class RouteGuideClient {

    private static final Logger logger = Logger.getLogger(RouteGuideClient.class.getName());

    public static void main(String[] args) throws InterruptedException {
        List<Feature> features;
        try {
            features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile().get());
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        RouteGuideClient client = new RouteGuideClient("localhost", 8980);
        try {
            // Looking for a valid feature
            client.getFeature(409146138, -746188906);

            // Feature missing.
            client.getFeature(0, 0);

            // Looking for features between 40, -75 and 42, -73.
            //client.listFeatures(400000000, -750000000, 420000000, -730000000);

            // Record a few randomly selected points from the features file.
            //client.recordRoute(features, 10);

            // Send and receive some notes.
            //client.routeChat();
        } finally {
            client.shutdown();
        }
    }

    private final ManagedChannel channel;
    private final RouteGuideBlockingStub blockingStub;
    private final RouteGuideStub asyncStub;

    /**
     * Construct client for accessing RouteGuide server at {@code host:port}.
     */
    public RouteGuideClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    /**
     * Construct client for accessing RouteGuide server using the existing channel.
     */
    public RouteGuideClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = RouteGuideGrpc.newBlockingStub(channel);
        asyncStub = RouteGuideGrpc.newStub(channel);
    }

    /**
     * Blocking unary call example.  Calls getFeature and prints the response.
     */
    public void getFeature(int lat, int lon) {
        info("*** GetFeature: lat={0} lon={1}", lat, lon);

        Point request = Point.newBuilder().setLatitude(lat).setLongitude(lon).build();

        Feature feature;
        try {
            feature = blockingStub.getFeature(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        if (RouteGuideUtil.exists(feature)) {
            info("Found feature called \"{0}\" at {1}, {2}",
                 feature.getName(),
                 RouteGuideUtil.getLatitude(feature.getLocation()),
                 RouteGuideUtil.getLongitude(feature.getLocation()));
        } else {
            info("Found no feature at {0}, {1}",
                 RouteGuideUtil.getLatitude(feature.getLocation()),
                 RouteGuideUtil.getLongitude(feature.getLocation()));
        }
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void info(String msg, Object... params) {
        logger.log(Level.INFO, msg, params);
    }
}