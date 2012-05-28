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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class Utilities {

    /**
     * Downloads a file
     * @param urlString URL to download the file from
     * @param file File to download to
     * @param text Thing being downloaded
     * @return If successful
     */
    public static boolean downloadFile(final String urlString, final File file, final String text) {
        Console.progress(text, 0);
        try {
            final URL url = new URL(urlString);
            final int contentLength = url.openConnection().getContentLength();
            final BufferedInputStream input = new BufferedInputStream(url.openStream());
            final FileOutputStream output = new FileOutputStream(file);
            final byte data[] = new byte[1024];
            int count, downloadedBytes = 0;
            while ((count = input.read(data, 0, 1024)) != -1) {
                downloadedBytes += count;
                Console.progress(text, (int) Math.round((double) downloadedBytes / (double) contentLength * 100D));
                output.write(data, 0, count);
            }
            Console.newLine();
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gets the contents of a URL
     * @param urlString URL to get the contents of
     * @return Contents of the URL
     */
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

    /**
     * Gets the MD5 of a file
     * @param file File to get the MD5 of
     * @return MD5 of the file
     */
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
    
    /**
     * Reads a String from an ObjectInputStream
     * @param stream Stream to read from
     * @return String that was read
     * @throws IOException If there was an error reading the String
     */
    public static String readString(ObjectInputStream stream) throws IOException {
        short size = stream.readShort();
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            builder.append(stream.readChar());
        }
        return builder.toString();
    }

    /**
     * Writes a String to an ObjectOutputStream
     * @param stream Stream to write to
     * @param string String to write
     * @throws IOException If there was an error writing the String
     */
    public static void writeString(ObjectOutputStream stream, String string) throws IOException {
        stream.writeShort((short) string.length());
        for (int i = 0; i < string.length(); i++) {
            stream.writeChar(string.charAt(i));
        }
    }
}
