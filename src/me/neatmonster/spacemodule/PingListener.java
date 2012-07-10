/*
 * This file is part of SpaceModule (http://spacebukkit.xereo.net/).
 *
 * SpaceModule is free software: you can redistribute it and/or modify it under the terms of the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license as published by the Creative
 * Common organization, either version 3.0 of the license, or (at your option) any later version.
 *
 * SpaceBukkit is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA) license for more details.
 *
 * You should have received a copy of the Attribution-NonCommercial-ShareAlike Unported (CC BY-NC-SA)
 * license along with this program. If not, see <http://creativecommons.org/licenses/by-nc-sa/3.0/>.
 */
package me.neatmonster.spacemodule;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

import me.neatmonster.spacemodule.utilities.Utilities;

/**
 * Pings the RTK and Plugin to ensure they are functioning correctly
 */
public class PingListener extends Thread {
    public static final long PING_EVERY = 30000; // Thirty seconds
    public static final long REQUEST_BUFFER = 10000; // Ten seconds

    private long lastPluginPing;
    private long lastRTKPing;
    private long lastPluginResponse;
    private long lastRTKResponse;

    private PacketSendClass pluginSender;
    private PacketReceiveClass pluginReceiver;
    private PacketSendClass rtkSender;
    private PacketReceiveClass rtkReceiver;

    private boolean lostRTK;
    private boolean lostPlugin;

    private AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Creates a new PingListener
     */
    public PingListener() {
        this.lostRTK = false;
        this.lostPlugin = false;
    }

    /**
     * Starts the Ping Listener
     */
    public void startup() {
        this.running.set(true);
        this.start();
    }

    @Override
    public void run() {
        try {
            pluginSender = new PacketSendClass(2014);
            pluginReceiver = new PacketReceiveClass(2014);
            rtkSender = new PacketSendClass(2013);
            rtkReceiver = new PacketReceiveClass(2013);
        } catch (SocketException e) {
            handleException(e, "Error starting the PingListener, socket error!");
        }
        while (running.get()) {

        }
    }

    /**
     * Sends packets to the module
     */
    private class PacketSendClass extends Thread {
        private final DatagramSocket socket;
        private final int port;

        /**
         * Creates a new PacketSendClass
         * 
         * @param port
         *            Port to listen on
         * 
         * @throws SocketException
         *             If the socket could not be created
         */
        public PacketSendClass(int port) throws SocketException {
            socket = new DatagramSocket(port);
            this.port = port;
        }

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (port == 2013) {
                if (now - lastPluginPing > PING_EVERY) {
                    try {
                        byte[] buffer = Utilities.longToBytes(now);
                        DatagramPacket packet = new DatagramPacket(buffer,
                                buffer.length, InetAddress.getLocalHost(), 2014);
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else if (port == 2014) {
                if (now - lastRTKPing > PING_EVERY) {
                    try {
                        byte[] buffer = Utilities.longToBytes(now);
                        DatagramPacket packet = new DatagramPacket(buffer,
                                buffer.length, InetAddress.getLocalHost(), 2014);
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Receives packets from the module
     */
    private class PacketReceiveClass extends Thread {
        private final DatagramSocket socket;
        private final int port;

        /**
         * Creates a new PacketReceiveClass
         * 
         * @param port
         *            Port to listen on
         * 
         * @throws SocketException
         *             If the socket could not be created
         */
        public PacketReceiveClass(int port) throws SocketException {
            socket = new DatagramSocket(port);
            this.port = port;
        }

        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (now - lastPluginPing > PING_EVERY) {
                try {
                    byte[] buffer = new byte[65536];
                    DatagramPacket packet = new DatagramPacket(buffer,
                            buffer.length, InetAddress.getLocalHost(), 2014);
                    socket.receive(packet);
                    long sent = Utilities.bytesToLong(packet.getData());
                    if (port == 2013) {
                        if (lastRTKResponse < sent) {
                            lastRTKResponse = sent;
                        }
                    } else if (port == 2014) {
                        if (lastPluginResponse < sent) {
                            lastPluginResponse = sent;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Shuts down the Ping Listener
     */
    public void shutdown() {
        this.running.set(false);
        try {
            pluginSender.join(1000);
            pluginReceiver.join(1000);
            rtkSender.join(1000);
            rtkReceiver.join(1000);
        } catch (InterruptedException e) {
            handleException(e, "Could not shutdown the PingListener!");
        }
    }

    /**
     * Called when an exception is thrown
     * 
     * @param e
     *            Exception thrown
     */
    public void handleException(Exception e, String reason) {
        shutdown();
        System.err.println("[SpaceBukkit] Ping Listener Error!");
        System.err.println(reason);
        System.err.println("Error message:");
        e.printStackTrace();
    }

    /**
     * Called when the RTK can't be found
     */
    public void onRTKNotFound() {
        if (lostRTK) {
            return;
        }
        System.err.println("[SpaceBukkit] Unable to ping the RTK!");
        System.err
                .println("[SpaceBukkit] Please ensure the correct ports are open");
        System.err
                .println("[SpaceBukkit] Please contact the forums (http://forums.xereo.net/) or IRC (#SpaceBukkit on irc.esper.net)");
        lostRTK = true;
    }

    /**
     * Called when the plugin can't be found
     */
    public void onPluginNotFound() {
        if (lostPlugin) {
            return;
        }
        System.err.println("[SpaceBukkit] Unable to ping the Plugin!");
        System.err
                .println("[SpaceBukkit] Please ensure the correct ports are open");
        System.err
                .println("[SpaceBukkit] Please contact the forums (http://forums.xereo.net/) or IRC (#SpaceBukkit on irc.esper.net)");
        lostPlugin = true;
    }

}
