package me.neatmonster.spacemodule;

import com.drdanick.McRKit.Wrapper;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This will periodically ping SpaceRTK and SpaceBukkit to see if they are online
 * @author Jamy
 */
public class NewPingListener {
    Thread pingT;
    
    private Boolean running;
    private Boolean RTKon;
    private Boolean SBon;

    
    /**
     * Da ping listenah
     */
    public NewPingListener() {
        
    }
    
    /**
     * Starts the ping thread
     */
    public void start() {
        pingT = new Thread(){
            public void run() {
                while (true) {
                    pingRTK();
                    pingSB();
                    try {
                        Thread.sleep(600 * 1000);
                    } catch (InterruptedException ex) {
                        handleException(ex, "Couldn't sleep the RTK thread!");
                    }
                }
            }
        };
        pingT.start();
        running = true;
    }
    
    /**
     * Stops the ping thread
     */
    public void stop() {
        try {
            pingT.wait();
        } catch (InterruptedException ex) {
            handleException(ex, "Thread interrupted!");
        }
        running = false;
    }
    
    /**
     * Pings the SpaceRTK module and excecutes onRTKdown() if SpaceRTK is down
     */
    public void pingRTK() {
        if (running) {
            URL                url; 
            URLConnection      urlConn; 
            DataInputStream    dis;
            try {
                url = new URL("http://localhost:"+SpaceModule.getInstance().rPort+"/ping");
                urlConn = url.openConnection(); 
                urlConn.setDoInput(true); 
                urlConn.setUseCaches(false);
                dis = new DataInputStream(urlConn.getInputStream()); 
                String s;
                s = dis.readLine();
                if (s != null) {
                    if (s.equals("Pong!")) {
                        RTKon = true;
                    } else {
                        RTKon = false;
                        onRTKdown();
                    }
                }
                dis.close();
            } catch (MalformedURLException e1) {
                handleException(e1, "The URL is not correct..");
            } catch (IOException e1) {
                handleException(e1, "Some IO went wrong..");
            }
        }
    }
    
    /**
     * Pings the SpaceBukkit plugin and excecutes onSBdown() if SpaceBukkit is down
     */
    public void pingSB() {
        if (running && isRunning()) {
            URL                url; 
            URLConnection      urlConn; 
            DataInputStream    dis;
            try {
                url = new URL("http://localhost:"+SpaceModule.getInstance().port+"/ping");
                urlConn = url.openConnection(); 
                urlConn.setDoInput(true); 
                urlConn.setUseCaches(false);
                dis = new DataInputStream(urlConn.getInputStream()); 
                String s;
                s = dis.readLine();
                if (s != null) {
                    if (s.equals("Pong!")) {
                        SBon = true;
                    } else {
                        SBon = false;
                        onSBdown();
                    }
                }
                dis.close();
            } catch (MalformedURLException e1) {
                handleException(e1, "The URL is not correct..");
            } catch (IOException e1) {
                handleException(e1, "Some IO went wrong..");
            }
        }
    }
    
    /**
     * If RTK is down, try to reload it
     */
    public void onRTKdown() {
        if (!RTKon) {
            SpaceModule.getInstance().unload();
            File artifact = new File("plugins" + File.separator + "space" + SpaceModule.getInstance().type.toLowerCase()+".jar");
            SpaceModule.getInstance().load(artifact);
        }
    }
    
    /**
     * If SpaceBukkit is down, display an error message
     */
    public void onSBdown() {
        if (!SBon && isRunning()) {
                System.err.println("Couldn't connect to the Plugin, it is possible that SpaceBukkit won't work!");
        }
    }
    
    /**
     * Checks if the server is running
     * @return If the server is running
     */
    public static boolean isRunning() {
        try {
            final Field field = Wrapper.getInstance().getClass().getDeclaredField("serverRunning");
            field.setAccessible(true);
            return (Boolean) field.get(Wrapper.getInstance());
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Called when an exception is thrown
     *
     * @param e
     *            Exception thrown
     */
    public void handleException(Exception e, String reason) {
        stop();
        System.err.println("[SpaceBukkit] New Ping Listener Error!");
        System.err.println(reason);
        System.err.println("Error message:");
        e.printStackTrace();
    }
}