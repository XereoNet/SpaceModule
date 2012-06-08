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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import me.neatmonster.spacemodule.utilities.Utilities;

/**
 * Pings the RTK and Plugin to ensure they are functioning correctly
 */
public class PingListener extends Thread {
    public static final long PING_EVERY = 30000; // Thirty seconds
    public static final long REQUEST_BUFFER = 10000; // Ten seconds
    public static final int WAIT_FOR_REQUEST = 90000; // One and a half minutes

    private final ServerSocket rtkServerSocket;
    private final ServerSocket pluginServerSocket;

    private Socket rtkSocket;
    private Socket pluginSocket;

    private long lastPluginPing;
    private long lastRTKPing;
    private long lastPluginResponse;
    private long lastRTKResponse;
    
    private boolean lostRTK;
    private boolean lostPlugin;

    private AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Creates a new PingListener
     * 
     * @throws IOException
     *             If an exception is thrown
     */
    public PingListener() throws IOException {
        this.rtkServerSocket = new ServerSocket(2013);
        this.pluginServerSocket = new ServerSocket(2014);
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
        if (rtkSocket == null) {
            try {
                rtkServerSocket.setSoTimeout(WAIT_FOR_REQUEST);
                rtkSocket = rtkServerSocket.accept();
            } catch (SocketTimeoutException e) {
                onRTKNotFound();
                shutdown();
                return;
            } catch (IOException e) {
                handleException(e, "Connection could not be established with the RTK!");
            }
        }
        if (pluginSocket == null) {
            try {
                pluginServerSocket.setSoTimeout(WAIT_FOR_REQUEST);
                pluginSocket = pluginServerSocket.accept();
            } catch (SocketTimeoutException e) {
                onPluginNotFound();
                shutdown();
                return;
            } catch (IOException e) {
                handleException(e, "Connection could not be established with the Plugin!");
            }
        }
        while (running.get()) {
            long now = System.currentTimeMillis();
            boolean shouldRead = now - lastPluginResponse > PING_EVERY || now - lastRTKResponse > PING_EVERY;
            if (shouldRead) {
                try {
                    ObjectInputStream pluginStream = new ObjectInputStream(pluginSocket.getInputStream());
                    ObjectInputStream rtkStream = new ObjectInputStream(rtkSocket.getInputStream());
                    String pluginInput = Utilities.readString(pluginStream);
                    String rtkInput = Utilities.readString(rtkStream);
                    if (pluginInput != null) {
                        parse(pluginInput);
                    }
                    if (rtkInput != null) {
                        parse(rtkInput);
                    }
                } catch (IOException e) {
                    // Do Nothing, as this means that there was no input sent
                }
            }
            if (now - lastPluginPing > PING_EVERY
                    && SpaceModule.isServerRunning()) {
                try {
                    ObjectOutputStream stream = new ObjectOutputStream(pluginSocket.getOutputStream());
                    Utilities.writeString(stream, "PING");
                    lastPluginPing = now;
                    stream.flush();
                } catch (IOException e) {
                    handleException(e, "Ping could not be sent to the Plugin!");
                }
            }
            if (now - lastRTKPing > PING_EVERY) {
                try {
                    ObjectOutputStream stream = new ObjectOutputStream(rtkSocket.getOutputStream());
                    Utilities.writeString(stream, "PING");
                    lastRTKPing = now;
                    stream.flush();
                } catch (IOException e) {
                    handleException(e, "Ping could not be sent to the RTK!");
                }
            }
            if (!lostPlugin && now - lastPluginResponse > PING_EVERY + REQUEST_BUFFER
                    && SpaceModule.isServerRunning()) {
                onPluginNotFound();
                lostPlugin = true;
            }
            if (!lostRTK && now - lastRTKResponse > PING_EVERY + REQUEST_BUFFER) {
                onRTKNotFound();
                lostRTK = true;
            }
        }
    }

    /**
     * Parses input from the Plugin or RTK
     * 
     * @param input
     *            Input from the Plugin or RTK
     */
    public void parse(String input) {
        long now = System.currentTimeMillis();
        if (input.equalsIgnoreCase("RTK-PING")) {
            lastRTKResponse = now;
        } else if (input.equalsIgnoreCase("PLUGIN-PING")) {
            lastPluginResponse = now;
        } else {
            System.err.println("[SpaceBukkit] Unknown input! '" + input
                    + "'.  Please report this to the developers");
        }
    }

    /**
     * Shuts down the Ping Listener
     */
    public void shutdown() {
        try {
            this.running.set(false);
            if (rtkSocket != null && !(rtkSocket.isClosed())) {
                rtkSocket.close();
            }
            if (pluginSocket != null && !(pluginSocket.isClosed())) {
                pluginSocket.close();
            }
           rtkServerSocket.close();
           pluginServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
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
        System.err.println("[SpaceBukkit] Unable to ping the RTK!");
        System.err
                .println("[SpaceBukkit] Please ensure the correct ports are open");
        System.err
                .println("[SpaceBukkit] Please contact the forums (http://forums.xereo.net/) or IRC (#SpaceBukkit on irc.esper.net)");
    }

    /**
     * Called when the plugin can't be found
     */
    public void onPluginNotFound() {
        System.err.println("[SpaceBukkit] Unable to ping the Plugin!");
        System.err
                .println("[SpaceBukkit] Please ensure the correct ports are open");
        System.err
                .println("[SpaceBukkit] Please contact the forums (http://forums.xereo.net/) or IRC (#SpaceBukkit on irc.esper.net)");
    }

}
