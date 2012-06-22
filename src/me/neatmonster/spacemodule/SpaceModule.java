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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import me.neatmonster.spacemodule.management.ImprovedClassLoader;
import me.neatmonster.spacemodule.management.VersionsManager;
import me.neatmonster.spacemodule.utilities.Console;
import me.neatmonster.spacemodule.utilities.Utilities;

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

    /**
     * Gets an instance of the SpaceModule
     * @return SpaceModule instance
     */
    public static SpaceModule getInstance() {
        return instance;
    }

    public String               type            = null;
    public boolean              development     = false;
    public boolean              recommended     = false;
    public String               artifactPath    = null;
    
    public int spaceBukkitPort;
    public int spaceRTKPort;

    public Timer                timer           = new Timer();
    public Object               spaceRTK        = null;
    public ImprovedClassLoader  classLoader     = null;
    public VersionsManager      versionsManager = null;

    private EventDispatcher     edt;
    private ToolkitEventHandler eventHandler;
    private PingListener pingListener;

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
        System.out.print("Done.\nLoading SpaceModule...");
    }

    /**
     * Starts the Module
     * @param versionsManager Version manager
     * @param firstTime If this is the first creation
     */
    public void execute(final VersionsManager versionsManager, final boolean firstTime) {
        File artifact = null;
        if (type.equals("Bukkit")) {
            artifact = new File("plugins", versionsManager.ARTIFACT_NAME);
        }
        if (artifact == null) {
            return;
        }
        if (!artifact.exists()) {
            update(versionsManager, artifact, firstTime);
        }
        else {
            try {
                final String md5 = Utilities.getMD5(artifact);
                final int buildNumber = versionsManager.match(md5);
                if (recommended && buildNumber != versionsManager.RECOMMENDED || development
                        && buildNumber != versionsManager.DEVELOPMENT)
                    update(versionsManager, artifact, firstTime);
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
            if (versionsManager == null) {
                versionsManager = new VersionsManager("Space" + type);
                versionsManager.setup();
            }
            File artifact;
            if (development || recommended)
                artifact = new File("plugins" + File.separator + versionsManager.ARTIFACT_NAME);
            else
                artifact = new File(artifactPath);
            if (versionsManager.match(Utilities.getMD5(artifact)) != 0)
                return "#" + versionsManager.match(Utilities.getMD5(artifact));
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
            final VersionsManager spaceModuleVersionsManager = new VersionsManager("SpaceModule");
            spaceModuleVersionsManager.setup();
            final File artifact = new File("toolkit" + File.separator + "modules",
                    spaceModuleVersionsManager.ARTIFACT_NAME);
            if (spaceModuleVersionsManager.match(Utilities.getMD5(artifact)) != 0)
                return "#" + spaceModuleVersionsManager.match(Utilities.getMD5(artifact));
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
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(CONFIGURATION);
        configuration.options().header(
                "#                !!!ATTENTION!!!                #\n" +
                "#   IF YOU CHANGE THE SALT, YOU MUST RESTART    #\n" +
                "#  THE WRAPPER FOR THE CHANGES TO TAKE EFFECT   #\n");

        type = configuration.getString("SpaceModule.Type", "Bukkit");
        configuration.set("SpaceModule.Type", type = "Bukkit");
        recommended = configuration.getBoolean("SpaceModule.Recommended", true);
        development = configuration.getBoolean("SpaceModule.Development", false);
        artifactPath = configuration.getString("SpaceModule.Artifact", "<automatic>");
        spaceBukkitPort = configuration.getInt("SpaceBukkit.port", 2011);
        spaceRTKPort = configuration.getInt("SpaceRTK.port", 2012);
        if (recommended && development)
            configuration.set("SpaceModule.Recommended", recommended = false);
        try {
            configuration.save(CONFIGURATION);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        unload();
        edt.setRunning(false);
        pingListener.shutdown();
        synchronized (edt) {
            edt.notifyAll();
        }
        eventHandler.setEnabled(false);
        instance = null;
    }

    @Override
    public void onEnable() {
        Console.header("SpaceModule v0.1");
        if (!MAIN_DIRECTORY.exists())
            MAIN_DIRECTORY.mkdir();
        if (!CONFIGURATION.exists())
            try {
                CONFIGURATION.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        loadConfiguration();
        try {
            pingListener = new PingListener();
            pingListener.startup();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (recommended || development) {
            versionsManager = new VersionsManager("Space" + type);
            versionsManager.setup();
            execute(versionsManager, true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Console.header("SpaceModule v0.1");
                    versionsManager.setup();
                    execute(versionsManager, false);
                    Console.footer();
                }
            }, 21600000L, 21600000L);
            final File artifact = new File("plugins" + File.separator + versionsManager.ARTIFACT_NAME);
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
     * @param versionsManager VersionManager
     * @param artifact Artifact to update to
     * @param firstTime If this is the first run
     */
    private void update(final VersionsManager versionsManager, final File artifact, final boolean firstTime) {
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
            url = "http://dev.drdanick.com/jenkins/job/Space" + type + "/Recommended/artifact/target/"
                    + versionsManager.ARTIFACT_NAME;
        else
            url = "http://dev.drdanick.com/jenkins/job/Space" + type + "/lastStableBuild/artifact/target/"
                    + versionsManager.ARTIFACT_NAME;
        if (spaceRTK != null)
            unload();
        if (artifact.exists())
            artifact.delete();
        Utilities.downloadFile(url, artifact, "Updating SpaceBukkit");
        if (!firstTime && wasRunning)
            Wrapper.getInstance().performAction(ToolkitAction.UNHOLD, null);
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
