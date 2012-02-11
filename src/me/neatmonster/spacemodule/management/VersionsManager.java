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

import java.util.LinkedHashMap;

import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacemodule.utilities.Utilities;

import org.bukkit.util.config.Configuration;

@SuppressWarnings("deprecation")
public class VersionsManager {
    public final String                          PROJECT_NAME;
    public String                                ARTIFACT_NAME;

    public int                                   RECOMMENDED;
    public int                                   DEVELOPMENT;

    private final LinkedHashMap<Integer, String> builds = new LinkedHashMap<Integer, String>();

    public VersionsManager(final String projectName) {
        PROJECT_NAME = projectName;
    }

    public int match(final String md5) {
        if (builds.containsValue(md5))
            for (final int buildNumber : builds.keySet()) {
                final String bMD5 = builds.get(buildNumber);
                if (bMD5.equalsIgnoreCase(md5))
                    return buildNumber;
            }
        return 0;
    }

    public void setup() {
        final String artifactPage = Utilities.getContent("http://dev.drdanick.com/jenkins/job/" + PROJECT_NAME + "/"
                + (SpaceModule.getInstance().recommended ? "Recommended" : "lastSuccessfulBuild") + "/");
        int beginIndex = artifactPage.indexOf(PROJECT_NAME.toLowerCase());
        final int endIndex = artifactPage.substring(beginIndex).indexOf(".jar") + beginIndex + 4;
        ARTIFACT_NAME = artifactPage.substring(beginIndex, endIndex);
        final String developmentPage = Utilities.getContent("http://dev.drdanick.com/jenkins/job/" + PROJECT_NAME
                + "/lastSuccessfulBuild/buildNumber/");
        DEVELOPMENT = Integer.parseInt(developmentPage);
        final String recommendedPage = Utilities.getContent("http://dev.drdanick.com/jenkins/job/" + PROJECT_NAME
                + "/Recommended/buildNumber/");
        RECOMMENDED = Integer.parseInt(recommendedPage);
        final Configuration database = new Configuration(SpaceModule.DATABASE);
        database.load();
        final int lastChecked = database.getInt(PROJECT_NAME + ".LastChecked", 0);
        if (lastChecked > 0)
            for (int buildNumber = 1; buildNumber < lastChecked + 1; buildNumber++) {
                final String md5 = database.getString(PROJECT_NAME + ".Build" + buildNumber, null);
                builds.put(buildNumber, md5);
            }
        for (int buildNumber = lastChecked + 1; buildNumber < DEVELOPMENT + 1; buildNumber++) {
            final String buildPage = Utilities.getContent("http://dev.drdanick.com/jenkins/job/" + PROJECT_NAME + "/"
                    + buildNumber + "/artifact/target/" + ARTIFACT_NAME + "/*fingerprint*/");
            if (buildPage != null) {
                beginIndex = buildPage.indexOf("<div class=\"md5sum\">MD5: ") + 25;
                final String md5 = buildPage.substring(beginIndex, beginIndex + 32);
                builds.put(buildNumber, md5);
                database.setProperty(PROJECT_NAME + ".Build" + buildNumber, md5);
            }
        }
        database.setProperty(PROJECT_NAME + ".LastChecked", DEVELOPMENT);
        database.save();
    }
}
