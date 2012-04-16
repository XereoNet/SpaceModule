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

public class Console {
    public static int    WIDTH    = 58;
    public static String TEMPLATE = "| (text)... [(bars)] (percentage) % |";

    public static void footer() {
        for (int a = 0; a < WIDTH; a++)
            System.out.print("-");
        System.out.print("\n");
    }

    public static void header(final String title) {
        final int dashes = WIDTH - title.length() - 2;
        final int left = (int) Math.ceil(dashes / 2D);
        final int right = (int) Math.floor(dashes / 2D);
        for (int a = 0; a < left; a++)
            System.out.print("-");
        System.out.print("[" + title + "]");
        for (int a = 0; a < right; a++)
            System.out.print("-");
        System.out.print("\n");
    }

    public static void newLine() {
        System.out.print("\n");
    }

    public static void progress(String text, final int percentage) {
        if (text.length() > 20)
            text = text.substring(0, 20);
        while (text.length() < 20)
            text += " ";
        final long bars = Math.round(percentage / 100D * 22D);
        String barsString = "";
        for (int a = 1; a < 23; a++)
            if (a < bars + 1)
                barsString += "|";
            else
                barsString += " ";
        String percentageString = "";
        if (percentage < 10)
            percentageString = "  " + percentage;
        else if (percentage < 100)
            percentageString = " " + percentage;
        else
            percentageString = "" + percentage;
        final String string = TEMPLATE.replace("(text)", text).replace("(bars)", barsString)
                .replace("(percentage)", percentageString);
        System.out.print(string + "\r");
    }

    public static void timedProgress(final String text, final int start, final int end, final long time) {
        final long interval = Math.round((double) time / (double) (end - start));
        for (int a = start; a < end + 1; a++) {
            progress(text, a);
            try {
                Thread.sleep(interval);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
