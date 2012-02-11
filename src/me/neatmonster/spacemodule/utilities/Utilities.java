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
package me.neatmonster.spacemodule.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class Utilities {

    public static boolean downloadFile(final String urlString, final File file) {
        try {
            final URL url = new URL(urlString);
            final ReadableByteChannel input = Channels.newChannel(url.openStream());
            final FileOutputStream output = new FileOutputStream(file);
            output.getChannel().transferFrom(input, 0, 1 << 24);
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getContent(final String urlString) {
        try {
            final URL url = new URL(urlString);
            final URLConnection urlConnection = url.openConnection();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String inputLine = "", content = "";
            while ((inputLine = reader.readLine()) != null)
                content += inputLine;
            reader.close();
            return content;
        } catch (final Exception e) {}
        return null;
    }

    public static String getMD5(final File file) {
        try {
            if (file.exists() && file.isFile() && file.canRead()) {
                final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                final DigestInputStream inputStream = new DigestInputStream(new FileInputStream(file), messageDigest);
                inputStream.on(true);
                while (inputStream.read() != -1) {}
                final byte[] bytes = messageDigest.digest();
                final StringBuilder md5 = new StringBuilder(bytes.length * 2);
                for (final byte b : bytes) {
                    if (b <= 0x0F && b >= 0x00)
                        md5.append('0');
                    md5.append(String.format("%x", b));
                }
                return md5.toString().toLowerCase();
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
