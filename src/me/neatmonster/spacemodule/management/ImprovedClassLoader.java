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
package me.neatmonster.spacemodule.management;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.jar.JarFile;

/**
 * Improved version of the URLClassLoader
 */
public class ImprovedClassLoader extends URLClassLoader {

    /**
     * Creates a new ImprovedClassLoader
     * @param urls URLs to load
     * @param parent Parent ClassLoader
     */
    public ImprovedClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
    }
    
    /**
     * Releases the classes fully
     */
    public void release() {
        try {
            final Field field = java.net.URLClassLoader.class.getDeclaredField("ucp");
            field.setAccessible(true);
            final Object mucp = field.get(this);
            final Field loaders = mucp.getClass().getDeclaredField("loaders");
            loaders.setAccessible(true);
            final Object collection = loaders.get(mucp);
            for (final Object sunMiscURLClassPathJarLoader : ((Collection<?>) collection).toArray())
                try {
                    final Field loader = sunMiscURLClassPathJarLoader.getClass().getDeclaredField("jar");
                    loader.setAccessible(true);
                    final Object jarFile = loader.get(sunMiscURLClassPathJarLoader);
                    ((JarFile) jarFile).close();
                } catch (final Throwable t) {}
        } catch (final Throwable t) {}
    }
}
