package me.neatmonster.spacemodule.management;

import com.drdanick.rtoolkit.util.config.ConfigurationFile;
import com.drdanick.rtoolkit.util.config.Node;
import me.neatmonster.spacemodule.SpaceModule;
import me.neatmonster.spacemodule.utilities.Console;
import me.neatmonster.spacemodule.utilities.Utilities;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.LinkedHashMap;

/**
 * Manages dependency artifacts.
 */
public class ArtifactManager {

    private String name;
    private String version;
    private String jenkinsURLBase;
    private int recommendedBuild = -1;
    private int developmentBuild = -1;
    private String buildAPIString;
    private String recommendedAPIString;
    private String artifactName;
    private boolean recommended;

    private final LinkedHashMap<Integer, String> builds = new LinkedHashMap<Integer, String>();


    public ArtifactManager(String name, String version, String jenkinsURLBase, boolean recommended) {
        this.name = name;
        this.version = version;
        this.recommended = recommended;
        this.jenkinsURLBase = jenkinsURLBase;
        buildAPIString = "/api/xml?tree=jobs[builds[number,artifacts[fileName],actions[levelValue]]]&wrapper=jenkins&xpath=//job/build[starts-with(artifact/fileName/text(),'"+name.toLowerCase()+"-"+version+"')]&exclude=//job/build/action[not(node())]|//job/build[not(artifact)]";
        recommendedAPIString = buildAPIString + "|//job/build[not(action/levelValue=4)]";
    }

    /**
     * Matches an MD5 with a build number
     * @param md5 MD5 to match
     * @return Build number, -1 if none
     */
    public int match(final String md5) {
        if (builds.containsValue(md5))
            for (final int buildNumber : builds.keySet()) {
                final String bMD5 = builds.get(buildNumber);
                if (bMD5 != null && bMD5.equalsIgnoreCase(md5))
                    return buildNumber;
            }
        return -1;
    }

    //XXX: Update engine is far too messy and needs to be rethought.
    public void setup(boolean printProgress, int progressMin, int progressMax) {
        double progress = progressMin;
        updateProgress(printProgress, progress);
        YamlConfiguration database = YamlConfiguration.loadConfiguration(SpaceModule.DATABASE);

        Object artifactAPIResponse = SpaceModule.getXStream().fromXML(Utilities.getContent(jenkinsURLBase + buildAPIString));
        Object recommendedArtifactAPIResponse = SpaceModule.getXStream().fromXML(Utilities.getContent(jenkinsURLBase + recommendedAPIString));

        ConfigurationFile allBuilds = new ConfigurationFile(new Node(artifactAPIResponse));
        ConfigurationFile recommendedBuilds = new ConfigurationFile(new Node(recommendedArtifactAPIResponse));

        int lastCheckedBuild = database.getInt(name + ".lastChecked", 0);

        database.set(name + ".version", version);

        String lastBuild = allBuilds.getString("jenkins[0].number");
        String lastRecommendedBuild = recommendedBuilds.getString("jenkins[0].number");
        developmentBuild = Integer.parseInt(lastBuild.trim());
        try {
            if(lastRecommendedBuild != null)
                recommendedBuild = Integer.parseInt(lastRecommendedBuild.trim());
            else
                recommendedBuild = developmentBuild;
        } catch(NumberFormatException e) {
            recommendedBuild = developmentBuild;
        }

        if (lastCheckedBuild == developmentBuild) {//No need to update the artifact name
                artifactName = database.getString(name+"-"+version+".artifactName");
        } else {
            artifactName = (SpaceModule.getInstance().recommended ? recommendedBuilds : allBuilds).getString("jenkins[0].artifact.fileName");
            database.set(name+".artifactName", artifactName);
        }

        //Map build artifacts to hashes taken from jenkins
        double progressDiv = (double)(progressMax - progressMin)/(double)allBuilds.getList("jenkins").size();
        for(Object o : (recommended ? recommendedBuilds : allBuilds).getList("jenkins")) {
            ConfigurationFile c = new ConfigurationFile(new Node(o));
            int number = Integer.parseInt(c.getString("number"));

            final String buildPage = Utilities.getContent(jenkinsURLBase + "/job/" + name + "/"
                    + number + "/artifact/target/" + artifactName + "/*fingerprint*/");

            if (buildPage != null) {
                final int beginIndex = buildPage.indexOf("<div class=\"md5sum\">MD5: ") + 25;
                final String md5 = buildPage.substring(beginIndex, beginIndex + 32);
                progress += progressDiv;
                updateProgress(printProgress, progress);
                builds.put(number, md5);
                database.set(name + ".build" + number, md5);
            }
        }

        database.set(name + ".LastChecked", developmentBuild);
        try {
            database.save(SpaceModule.DATABASE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        database.set(name+".build" + ".lastChecked", developmentBuild);

        updateProgress(printProgress, progressMax);
    }

    public String getArtifactFileName() {
        return artifactName;
    }

    public String getJobName() {
        return name;
    }

    public int getRecommendedBuild() {
        return recommendedBuild;
    }

    public int getDevelopmentBuild() {
        return developmentBuild;
    }

    private void updateProgress(boolean printProgress, double p) {
        if(printProgress)
            Console.progress("Checking for updates", (int)p);
    }
}
