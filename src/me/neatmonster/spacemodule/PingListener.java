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
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import me.neatmonster.spacemodule.utilities.Utilities;

public class PingListener extends Thread {
    public static final long PING_EVERY = 1 * 1000;
    public static final long REQUEST_BUFFER = 5 * 100;

    private final Socket rtkSocket;
    private final Socket pluginSocket;
    private final Socket moduleSocket;

    private long lastPluginPing;
    private long lastRTKPing;
    private long lastPluginResponse;
    private long lastRTKResponse;

    private AtomicBoolean running;

    public PingListener() throws UnknownHostException, IOException,
            NoSuchFieldException, SecurityException, IllegalArgumentException,
            IllegalAccessException {
        Object spaceRTK = SpaceModule.getInstance().spaceRTK;
        Field port = spaceRTK.getClass().getField("port");
        Field rPort = spaceRTK.getClass().getField("rPort");
        this.rtkSocket = new Socket(InetAddress.getLocalHost(),
                rPort.getInt(spaceRTK));
        this.pluginSocket = new Socket(InetAddress.getLocalHost(),
                port.getInt(spaceRTK));
        this.moduleSocket = new Socket(InetAddress.getLocalHost(), 2010); // TODO
                                                                          // config
                                                                          // value?
    }
    
    public void startup() {
        this.running.set(true);
        this.start();
    }

    @Override
    public void run() {
        ObjectInputStream moduleStream = null;
        ObjectOutputStream pluginStream = null;
        ObjectOutputStream rtkStream = null;
        try {
            moduleStream = new ObjectInputStream(moduleSocket.getInputStream());
            pluginStream = new ObjectOutputStream(
                    pluginSocket.getOutputStream());
            rtkStream = new ObjectOutputStream(rtkSocket.getOutputStream());
        } catch (IOException e) {
            handleException(e);
        }
        while (running.get()) {
            long now = System.currentTimeMillis();
            String input = null;
            try {
                input = Utilities.readString(moduleStream);
            } catch (IOException e) {
                handleException(e);
            }
            if (input != null) {
                parse(input);
            }
            if (now - lastPluginPing > PING_EVERY
                    && SpaceModule.isServerRunning()) {
                try {
                    Utilities.writeString(pluginStream, "PING");
                    lastPluginPing = now;
                    pluginStream.flush();
                } catch (IOException e) {
                    handleException(e);
                }
            }
            if (now - lastRTKPing > PING_EVERY) {
                try {
                    Utilities.writeString(rtkStream, "PING");
                    lastRTKPing = now;
                    rtkStream.flush();
                } catch (IOException e) {
                    handleException(e);
                }
            }
            if (now - lastPluginResponse > PING_EVERY + REQUEST_BUFFER
                    && SpaceModule.isServerRunning()) {
                onPluginNotFound();
            }
            if (now - lastRTKResponse > PING_EVERY + REQUEST_BUFFER) {
                onRTKNotFound();
            }
        }
    }

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

    public void shutdown() {
        try {
            this.running.set(false);
            rtkSocket.close();
            pluginSocket.close();
            moduleSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleException(Exception e) {
        shutdown();
        System.err.println("[SpaceBukkit] Ping Listener Error! Error message:");
        e.printStackTrace();
    }

    public void onRTKNotFound() {
        System.err.println("[SpaceBukkit] Unable to ping SpaceRTK!");
        System.err
                .println("[SpaceBukkit] Please insure the correct ports are open");
        System.err
                .println("[SpaceBukkit] Please contact the forums (http://forums.xereo.net/) or IRC (#SpaceBukkit on irc.esper.net)");
    }

    public void onPluginNotFound() {
        System.err.println("[SpaceBukkit] Unable to ping the Plugin!");
        System.err
                .println("[SpaceBukkit] Please insure the correct ports are open");
        System.err
                .println("[SpaceBukkit] Please contact the forums (http://forums.xereo.net/) or IRC (#SpaceBukkit on irc.esper.net)");
    }

}
