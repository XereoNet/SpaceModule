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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import me.neatmonster.spacemodule.management.ArtifactManager;
import me.neatmonster.spacemodule.management.ImprovedClassLoader;
import me.neatmonster.spacemodule.utilities.Console;
import me.neatmonster.spacemodule.utilities.Utilities;

import me.neatmonster.spacemodule.utilities.XMLListConverter;
import me.neatmonster.spacemodule.utilities.XMLMapConverter;
import org.bukkit.configuration.file.YamlConfiguration;

import com.drdanick.McRKit.ToolkitAction;
import com.drdanick.McRKit.ToolkitEvent;
import com.drdanick.McRKit.Wrapper;
import com.drdanick.McRKit.module.Module;
import com.drdanick.McRKit.module.ModuleLoader;
import com.drdanick.McRKit.module.ModuleMetadata;
import com.drdanick.rtoolkit.EventDispatcher;
import com.drdanick.rtoolkit.event.ToolkitEventHandler;

/**
 * Main class of the Module
 */
public class SpaceModule extends Module {
    /**
     * Main Directory of the Module
     */
    public static final File   MAIN_DIRECTORY = new File("SpaceModule");
    /**
     * Configuration file of the Module
     */
    public static final File   CONFIGURATION  = new File(MAIN_DIRECTORY.getPath(), "configuration.yml");
    /**
     * Database file of the Module
     */
    public static final File   DATABASE       = new File(MAIN_DIRECTORY.getPath(), "cache.db");

    private static SpaceModule instance;

    private static XStream xstream;

    private static String version;

    private static String title;


    public String               type            = null;
    public boolean              development     = false;
    public boolean              recommended     = false;
    public String               artifactPath    = null;
    public String               salt            = null;
    public int                  port            = 0;
    public int                  rPort           = 0;
    public int                  pingPort        = 0;
    public InetAddress          bindAddress;

    public Timer                         timer            = new Timer();
    public Object                        spaceRTK         = null;
    public ImprovedClassLoader           classLoader      = null;
    public Map<String, ArtifactManager>  artifactManagers = null;

    private EventDispatcher     edt;
    private ToolkitEventHandler eventHandler;

    private boolean firstRun = false;


    static {
        xstream = new XStream(new DomDriver());
        SpaceModule.xstream.registerConverter(new XMLMapConverter(SpaceModule.xstream.getMapper()));
        SpaceModule.xstream.registerConverter(new XMLListConverter(SpaceModule.xstream.getMapper()));
        SpaceModule.xstream.alias("jenkins", List.class);
        SpaceModule.xstream.alias("job", List.class);
        SpaceModule.xstream.alias("build", Map.class);
        SpaceModule.xstream.alias("action", Map.class);
        SpaceModule.xstream.alias("lastBuiltRevision", Map.class);
        SpaceModule.xstream.alias("artifact", Map.class);
        SpaceModule.xstream.alias("number", String.class);
        SpaceModule.xstream.alias("name", String.class);

        version = SpaceModule.class.getPackage().getSpecificationVersion();
        title = SpaceModule.class.getPackage().getSpecificationTitle();
    }

    /**
     * Get the XStream instance associated with this SpaceModule instance.
     * @return The XStream instance associated with this SpaceModule instance.
     */
    public static XStream getXStream() {
        return xstream;
    }

    /**
     * Get the specification version of this SpaceModule instance.
     * @return the specification version of this spacemodule instance.
     */
    public static String getSpecificationVersion() {
        return version;
    }

    /**
     * Get the specification title of this SpaceModule instance.
     * @return the specification title of this SpaceModule instance.
     */
    public static String getSpecificationTitle() {
        return title;
    }

    /**
     * Gets an instance of the SpaceModule
     * @return SpaceModule instance
     */
    public static SpaceModule getInstance() {
        return instance;
    }

    /**
     * Creates a new SpaceModule
     * @param meta Module Metadata
     * @param moduleLoader Module loader
     * @param cLoader Class loader
     */
    public SpaceModule(final ModuleMetadata meta, final ModuleLoader moduleLoader, final ClassLoader cLoader) {
        super(meta, moduleLoader, cLoader, ToolkitEvent.ON_TOOLKIT_START, ToolkitEvent.NULL_EVENT);
        instance = this;
        edt = new EventDispatcher();
        eventHandler = new EventHandler();
        artifactManagers = new HashMap<String, ArtifactManager>();
        System.out.print("Done.\nLoading SpaceModule...");
    }

    /**
     * Starts the Module
     * @param artifactManager Version manager
     * @param firstTime If this is the first creation
     */
    public void execute(ArtifactManager artifactManager, boolean firstTime) {
        File artifact = null;
        if (type.equals("Bukkit")) {
            artifact = new File("plugins", "space" + type.toLowerCase() + ".jar");
        }
        if (artifact == null) {
            return;
        }
        if (!artifact.exists()) {
            update(artifactManager, artifact, firstTime);
        }
        else {
            try {
                final String md5 = Utilities.getMD5(artifact);
                final int buildNumber = artifactManager.match(md5);
                if (recommended && buildNumber != artifactManager.getRecommendedBuild() || development
                        && buildNumber != artifactManager.getDevelopmentBuild())
                    update(artifactManager, artifact, firstTime);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        Console.timedProgress("Starting SpaceBukkit", 0, 100, 500L);
        Console.newLine();
    }

    /**
     * Gets the version of the SpaceRTK
     * @return SpaceRTK version
     */
    public String getModuleVersion() {
        try {
            if (!artifactManagers.containsKey("Space" + type)) {
                ArtifactManager aMngr = new ArtifactManager("Space" + type, version, "http://dev.drdanick.com/jenkins", recommended); //TODO: URL base needs to go into the config
                artifactManagers.put("Space" + type, aMngr);
                aMngr.setup(true, 0, 100);
                Console.newLine();
            }
            File artifact;
            ArtifactManager artifactManager = artifactManagers.get("Space" + type);
            if (development || recommended)
                artifact = new File("plugins" + File.separator + artifactManager.getArtifactFileName());
            else
                artifact = new File(artifactPath);
            if (artifactManager.match(Utilities.getMD5(artifact)) != 0)
                return "#" + artifactManager.match(Utilities.getMD5(artifact));
            else
                return "#?";
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return "<unknown>";
    }

    /**
     * Gets the version of the Module
     * @return Module version
     */
    public String getVersion() {
        try {
            final ArtifactManager spaceModuleArtifactManager = new ArtifactManager("SpaceModule", version, "http://dev.drdanick.com/jenkins", recommended);
            spaceModuleArtifactManager.setup(true, 0, 100);
            Console.newLine();
            final File artifact = new File("toolkit" + File.separator + "modules",
                    spaceModuleArtifactManager.getArtifactFileName());
            if (spaceModuleArtifactManager.match(Utilities.getMD5(artifact)) != 0)
                return "#" + spaceModuleArtifactManager.match(Utilities.getMD5(artifact));
            else
                return "#?";
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return "<unknown>";
    }

    /**
     * Gets the EDT
     * @return EDT
     */
    public EventDispatcher getEdt() {
        return edt;
    }

    /**
     * Gets the ToolkitEventHandler
     * @return ToolkitEventHandler
     */
    public ToolkitEventHandler getEventHandler() {
        return eventHandler;
    }

    /**
     * Loads a file
     * @param jar File to load
     */
    private void load(final File jar) {
        try {
            final URL url = new URL("file:" + jar.getAbsolutePath());
            classLoader = new ImprovedClassLoader(new URL[] {url}, getClass().getClassLoader());
            final Class<?> loadedClass = classLoader.loadClass("me.neatmonster.spacertk.SpaceRTK");
            spaceRTK = loadedClass.getConstructor().newInstance();
            final Method onEnable = loadedClass.getMethod("onEnable");
            onEnable.invoke(spaceRTK);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the configuration
     */
    private void loadConfiguration() {
        if (!(CONFIGURATION.exists())) {
            firstRun = true;
            try {
                CONFIGURATION.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(CONFIGURATION);
        config.addDefault("SpaceModule.type", "Bukkit");
        config.addDefault("SpaceModule.recommended", true);
        config.addDefault("SpaceModule.development", false);
        config.addDefault("SpaceModule.artifact", "<automatic>");
        config.addDefault("SpaceBukkit.port", 2011);
        config.addDefault("SpaceBukkit.pingPort", 2014);
        config.addDefault("SpaceRTK.port", 2012);
        config.addDefault("SpaceRTK.pingPort", 2013);
        config.addDefault("General.backupDirectory", "Backups");
        config.addDefault("General.backupLogs", true);
        config.options().copyDefaults(true);
        config.options().header(
                "#                !!!ATTENTION!!!                #\n" +
                "#   IF YOU CHANGE THE SALT, YOU MUST RESTART    #\n" +
                "#  THE WRAPPER FOR THE CHANGES TO TAKE EFFECT   #\n");
        migrateConfig(config);
        salt = config.getString("General.salt", "<default>");
        if (salt.equals("<default>")) {
            salt = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
            config.set("General.salt", salt);
        }

        String bindAddressString = config.getString("General.bindIp", "0.0.0.0");
        if(bindAddressString.trim().isEmpty())
            bindAddressString = "0.0.0.0";
        try {
            bindAddress = InetAddress.getByName(bindAddressString);
        } catch(UnknownHostException e) {
            try {
                bindAddress = InetAddress.getLocalHost();
            } catch(UnknownHostException e2) {}
            System.err.println("Warning: Could not assign bind address " + bindAddressString + ":");
            System.err.println(e.getMessage());
            System.err.println("Will bind to loopback address: " + bindAddress.getHostAddress() + "...");
        }

        port = config.getInt("SpaceBukkit.port", 2011);
        pingPort = config.getInt("SpaceBukkit.pingPort", 2014);
        rPort = config.getInt("SpaceRTK.port", 2012);
        type = config.getString("SpaceModule.type", "Bukkit");
        config.set("SpaceModule.type", type = "Bukkit");
        recommended = config.getBoolean("SpaceModule.recommended", true);
        development = config.getBoolean("SpaceModule.development", false);
        artifactPath = config.getString("SpaceModule.artifact", "<automatic>");
        if (recommended && development) {
            config.set("SpaceModule.recommended", recommended = false);
        }
        try {
            config.save(CONFIGURATION);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        unload();
        edt.setRunning(false);
        synchronized (edt) {
            edt.notifyAll();
        }
        eventHandler.setEnabled(false);
        instance = null;
    }

    @Override
    public void onEnable() {
        Console.header("SpaceModule v"+getSpecificationVersion());
        if (!MAIN_DIRECTORY.exists()) {
            MAIN_DIRECTORY.mkdir();
        }
        if (!CONFIGURATION.exists()) {
            try {
                CONFIGURATION.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        loadConfiguration();

        if (recommended || development) {
            File pluginDir = new File("plugins");
            for(File f : pluginDir.listFiles()) {
                if(f.getName().matches("space"+type.toLowerCase()+"-[0-9]*\\.[0-9]*-[A-Za-z]*\\.jar"))
                    f.delete();
            }

            String jenkinsURL = "http://dev.drdanick.com/jenkins"; //TODO: this needs to go into the config
            artifactManagers.put("Space" + type, new ArtifactManager("Space" + type, version, jenkinsURL, recommended));
            double progressDiv = 100D / artifactManagers.size();
            int minProgress = 0;
            for(ArtifactManager m : artifactManagers.values()) {
                m.setup(true, minProgress, (int)(minProgress + progressDiv));
                minProgress += progressDiv;
            }
            Console.progress("Checking for updates", 100); //XXX: shouldn't call this here
            Console.newLine();
            for(final ArtifactManager m : artifactManagers.values()) {
                execute(m, true);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        Console.header("SpaceModule v"+getSpecificationVersion());
                        m.setup(true, 0, 100);
                        Console.newLine();
                        execute(m, false);
                        Console.footer();
                    }
                }, 21600000L + (long)(Math.random() * 43200000L), 21600000L); //Schedule updates at a period of 6 hours, starting from 6-18 hours after execution.
            }
            File artifact = new File("plugins" + File.separator + "space" + type.toLowerCase()+".jar");
            artifactPath = artifact.getPath();
            load(artifact);
        } else {
            final File artifact = new File(artifactPath);
            load(artifact);
            Console.timedProgress("Starting SpaceBukkit", 0, 100, 500L);
            Console.newLine();
        }

        if(!edt.isRunning()) {
            synchronized (edt) {
                edt.notifyAll();
            }
            Thread edtThread = new Thread(edt, "SpaceModule EventDispatcher");
            edtThread.setDaemon(true);
            edtThread.start();
        }

        eventHandler.setEnabled(true);
        if(!eventHandler.isRunning()) {
            Thread handlerThread = new Thread(eventHandler, "SpaceModule EventHandler");
            handlerThread.setDaemon(true);
            handlerThread.start();
        }

        if (firstRun)
            printConnectionInfo();

        Console.footer();

    }

    /**
     * Unloads the SpaceRTK
     */
    private void unload() {
        try {
            final Method onDisable = spaceRTK.getClass().getMethod("onDisable");
            onDisable.invoke(spaceRTK);
            spaceRTK = null;
            classLoader.release();
            classLoader = null;
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the SpaceRTK
     * @param artifactManager VersionManager
     * @param artifact Artifact to update to
     *
     * @param firstTime If this is the first run
     */
    private void update(ArtifactManager artifactManager, File artifact, boolean firstTime) {
        boolean wasRunning = false;
        if (!firstTime)
            try {
                final Field field = Wrapper.getInstance().getClass().getDeclaredField("serverRunning");
                field.setAccessible(true);
                wasRunning = (Boolean) field.get(Wrapper.getInstance());
                if (wasRunning)
                    Wrapper.getInstance().performAction(ToolkitAction.HOLD, null);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        String url;
        if (recommended)
            url = "http://dev.drdanick.com/jenkins/job/Space" + type + "/"+ artifactManager.getRecommendedBuild() +"/artifact/target/"
                    + artifactManager.getArtifactFileName();
        else
            url = "http://dev.drdanick.com/jenkins/job/Space" + type + "/"+ artifactManager.getDevelopmentBuild() +"/artifact/target/"
                    + artifactManager.getArtifactFileName();
        if (spaceRTK != null)
            unload();
        if (artifact.exists())
            artifact.delete();
        Utilities.downloadFile(url, new File(artifact.getParentFile(),"space"+type.toLowerCase()+".jar"), "Updating SpaceBukkit");
        if (!firstTime && wasRunning)
            Wrapper.getInstance().performAction(ToolkitAction.UNHOLD, null);
    }


    private void migrateConfig(YamlConfiguration config) {
        if (config.getString("SpaceModule.Type") == null) {
            return;
        }
        String type = config.getString("SpaceModule.Type");
        config.set("SpaceModule.Type", null);
        boolean recommended = config.getBoolean("SpaceModule.Recommended");
        config.set("SpaceModule.Recommended", null);
        boolean development = config.getBoolean("SpaceModule.Development");
        config.set("SpaceModule.Development", null);
        String artifact = config.getString("SpaceModule.Artifact");
        config.set("SpaceModule.Artifact", null);
        int port = config.getInt("SpaceBukkit.Port");
        config.set("SpaceBukkit.Port", null);
        int rPort = config.getInt("SpaceRTK.Port");
        config.set("SpaceRTK.Port", null);
        String salt = config.getString("General.Salt");
        config.set("General.Salt", null);
        String worldContainer = config.getString("General.WorldContainer");
        config.set("General.WorldContainer", null);

        config.set("SpaceModule.type", type);
        config.set("SpaceModule.recommended", recommended);
        config.set("SpaceModule.development", development);
        config.set("SpaceModule.artifact", artifact);
        config.set("SpaceBukkit.port", port);
        config.set("SpaceRTK.port", rPort);
        config.set("General.salt", salt);
        config.set("General.worldContainer", worldContainer);
    }

    /**
     * Checks if the server is running
     * @return If the server is running
     */
    public static boolean isServerRunning() {
        try {
            final Field field = Wrapper.getInstance().getClass().getDeclaredField("serverRunning");
            field.setAccessible(true);
            return (Boolean) field.get(Wrapper.getInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void printConnectionInfo() {
        BufferedReader in = null;

        try {
            URL whatismyip = new URL("http://automation.whatismyip.com/n09230945.asp");
            in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));

            String ip = in.readLine();
            System.out.println("Welcome to SpaceBukkit! Your connection information is: ");
            System.out.println("Salt: " + salt);
            System.out.println("External IP: " + ip);
            System.out.println("SpaceBukkit port: " + port);
            System.out.println("SpaceRTK port: " + rPort);
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in != null)
                    in.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Forces the ToolkitEventHandler to function correctly
     */
    private class EventHandler extends ToolkitEventHandler {
        /**
         * Creates a new EventHandler
         */
        public EventHandler() {
            setEnabled(true);
        }

    }
}
