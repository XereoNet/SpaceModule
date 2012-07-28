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
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pings the RTK and Plugin to ensure they are functioning correctly
 */
public class PingListener {
    public static final int PLUGIN_THRESHOLD = 20000; // Twenty seconds
    public static final int REQUEST_THRESHOLD = 60000; // Sixty seconds
    public static final int SLEEP_TIME = 30000; // Thirty seconds

    public DatagramSocket rtkSocket;
    public DatagramSocket pluginSocket;

    private boolean lostRTK;
    private boolean lostPlugin;

    private AtomicBoolean running = new AtomicBoolean(false);

    private InetAddress localHost;

    /**
     * Creates a new PingListener
     */
    public PingListener() {
        try {
            this.localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            handleException(e, "Unable to get the Local Host!");
        }
        this.lostRTK = false;
        this.lostPlugin = false;
        try {
            this.rtkSocket = new DatagramSocket(SpaceModule.getInstance().pingPort,
                    InetAddress.getLocalHost());
            this.pluginSocket = new DatagramSocket(SpaceModule.getInstance().rPingPort,
                    InetAddress.getLocalHost());
        } catch (IOException e) {
            handleException(e, "Unable to start the PingListener!");
        }
    }

    /**
     * Starts the Ping Listener
     */
    public void startup() {
        this.running.set(true);
        this.start();
    }

    /**
     * Starts the threads
     */
    private void start() {
        new RTKThread().start();
        new PluginThread().start();
    }

    /**
     * Shuts down the Ping Listener
     */
    public void shutdown() {
        this.running.set(false);
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

    private class RTKThread extends Thread {

        public RTKThread() {
            super("Ping Listener RTK Thread");
        }

        @Override
        public void run() {
            try {
                rtkSocket.setSoTimeout(REQUEST_THRESHOLD);
            } catch (SocketException e) {
                handleException(e, "Error setting the So Timeout!");
            }
            while (running.get()) {
                byte[] buffer = new byte[512];
                buffer[0] = 1;
                try {
                    DatagramPacket packet = new DatagramPacket(buffer,
                            buffer.length, localHost, SpaceModule.getInstance().rPingPort);
                    rtkSocket.receive(packet);
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        handleException(e, "Error sleeping in the run() loop!");
                    }
                    rtkSocket.send(packet);
                } catch (SocketTimeoutException e) {
                    onRTKNotFound();
                    try {
                        this.join(1000);
                    } catch (InterruptedException e1) {
                        handleException(e1, "Unable to stop the RTK PingListener!");
                    }
                } catch (IOException e) {
                    handleException(e,
                            "Error receiving and sending the RTK packet!");
                }

            }
        }
    }

    private class PluginThread extends Thread {
        private boolean first = true;

        public PluginThread() {
            super("Ping Listener Plugin Thread");
        }

        @Override
        public void run() {
            if (first) {
                try {
                    pluginSocket.setSoTimeout(PLUGIN_THRESHOLD + REQUEST_THRESHOLD);
                } catch (SocketException e) {
                    handleException(e, "Error setting the So Timeout!");
                }
                first = false;
            } else {
                try {
                    pluginSocket.setSoTimeout(REQUEST_THRESHOLD);
                } catch (SocketException e) {
                    handleException(e, "Error setting the So Timeout!");
                }
            }
            while (running.get()) {
                byte[] buffer = new byte[512];
                buffer[0] = 1;
                try {
                    DatagramPacket packet = new DatagramPacket(buffer,
                            buffer.length, localHost, SpaceModule.getInstance().pingPort);
                    pluginSocket.receive(packet);
                    try {
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        handleException(e, "Error sleeping in the run() loop!");
                    }
                    pluginSocket.send(packet);
                } catch (SocketTimeoutException e) {
                    onPluginNotFound();
                    try {
                        this.join(1000);
                    } catch (InterruptedException e1) {
                        handleException(e1, "Unable to stop the Plugin PingListener!");
                    }
                } catch (IOException e) {
                    handleException(e,
                            "Error receiving and sending the Plugin packet!");
                }

            }
        }
    }

}
