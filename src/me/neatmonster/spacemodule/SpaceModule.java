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
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import com.drdanick.rtoolkit.EventDispatcher;
import com.drdanick.rtoolkit.event.ToolkitEventHandler;
import me.neatmonster.spacemodule.management.ImprovedClassLoader;
import me.neatmonster.spacemodule.management.VersionsManager;
import me.neatmonster.spacemodule.utilities.Console;
import me.neatmonster.spacemodule.utilities.Utilities;

import org.bukkit.util.config.Configuration;

import com.drdanick.McRKit.ToolkitAction;
import com.drdanick.McRKit.ToolkitEvent;
import com.drdanick.McRKit.Wrapper;
import com.drdanick.McRKit.module.Module;
import com.drdanick.McRKit.module.ModuleLoader;
import com.drdanick.McRKit.module.ModuleMetadata;

@SuppressWarnings("deprecation")
public class SpaceModule extends Module {
    public static final File   MAIN_DIRECTORY = new File("SpaceModule");
    public static final File   CONFIGURATION  = new File(MAIN_DIRECTORY.getPath(), "configuration.yml");
    public static final File   DATABASE       = new File(MAIN_DIRECTORY.getPath(), "cache.db");

    private static SpaceModule instance;

    public static SpaceModule getInstance() {
        return instance;
    }

    public String               type            = null;
    public boolean              development     = false;
    public boolean              recommended     = false;
    public String               artifactPath    = null;

    public Timer                timer           = new Timer();
    public Object               spaceRTK        = null;
    public ImprovedClassLoader  classLoader     = null;
    public VersionsManager      versionsManager = null;

    private EventDispatcher     edt;
    private ToolkitEventHandler eventHandler;
    
    private PingListener pingListener;

    public SpaceModule(final ModuleMetadata meta, final ModuleLoader moduleLoader, final ClassLoader cLoader) {
        super(meta, moduleLoader, cLoader, ToolkitEvent.ON_TOOLKIT_START, ToolkitEvent.NULL_EVENT);
        instance = this;
        edt = new EventDispatcher();
        eventHandler = new EventHandler();
        try {
            pingListener = new PingListener();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.print("Done.\nLoading SpaceModule...");
    }

    public void execute(final VersionsManager versionsManager, final boolean firstTime) {
        File artifact = null;
        if (type.equals("Bukkit"))
            artifact = new File("plugins", versionsManager.ARTIFACT_NAME);
        if (!artifact.exists())
            update(versionsManager, artifact, firstTime);
        else
            try {
                final String md5 = Utilities.getMD5(artifact);
                final int buildNumber = versionsManager.match(md5);
                if (recommended && buildNumber != versionsManager.RECOMMENDED || development
                        && buildNumber != versionsManager.DEVELOPMENT)
                    update(versionsManager, artifact, firstTime);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        Console.timedProgress("Starting SpaceBukkit", 0, 100, 500L);
        Console.newLine();
    }

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

    public EventDispatcher getEdt() {
        return edt;
    }

    public ToolkitEventHandler getEventHandler() {
        return eventHandler;
    }

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

    private void loadConfiguration() {
        final Configuration configuration = new Configuration(CONFIGURATION);
        configuration.load();
        configuration.setHeader(
                "#                !!!ATTENTION!!!                #",
                "#   IF YOU CHANGE THE SALT, YOU MUST RESTART    #",
                "#  THE WRAPPER FOR THE CHANGES TO TAKE EFFECT   #");

        type = configuration.getString("SpaceModule.Type", "Bukkit");
        configuration.setProperty("SpaceModule.Type", type = "Bukkit");
        recommended = configuration.getBoolean("SpaceModule.Recommended", true);
        development = configuration.getBoolean("SpaceModule.Development", false);
        artifactPath = configuration.getString("SpaceModule.Artifact", "<automatic>");
        if (recommended && development)
            configuration.setProperty("SpaceModule.Recommended", recommended = false);
        configuration.save();
    }

    @Override
    public void onDisable() {
        unload();
        edt.setRunning(false);
        synchronized (edt) {
            edt.notifyAll();
        }
        eventHandler.setEnabled(false);
        pingListener.shutdown();
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
        pingListener.startup();
        if(!eventHandler.isRunning()) {
            Thread handlerThread = new Thread(eventHandler, "SpaceModule EventHandler");
            handlerThread.setDaemon(true);
            handlerThread.start();
        }

        Console.footer();
    }

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

    private void update(final VersionsManager versionsManager, final File artifact, final boolean firstTime) {
        boolean wasRunning = false;
        if (!firstTime)
            try {
                wasRunning = isServerRunning();
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

    private class EventHandler extends ToolkitEventHandler {

        public EventHandler() {
            setEnabled(true);
        }

    }
}
