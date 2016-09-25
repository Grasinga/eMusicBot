package net.grasinga.eNetMusic;

import com.jaunt.UserAgent;
import net.dv8tion.jda.entities.TextChannel;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Radio Class
 * Gets an internet radio station's information.
 *
 * @see eNetMusic#playRadio(TextChannel, String)
 */
public class Radio {

    /**
     * Gets the radio station's name from the url parameter.
     *
     * @param url Radio station's mp3 stream.
     * @return Radio Station's name.
     */
    public static String getStationName(String url) {return getInfo(url, "station");}

    /**
     * Gets the radio station's name from the url parameter.
     *
     * @param url Radio station's mp3 stream.
     * @return Current song's artist and title of radio station.
     */
    public static String getCurrentSong(String url){return getInfo(url, "song");}

    /**
     * Gets info from the radio station provided by the url parameter.
     *
     * @param url Radio station's mp3 stream.
     * @param type Type of info wanting to be returned.
     * @return Info based on type parameter.
     * @see #getStationName(String)
     * @see #getCurrentSong(String)
     */
    private static String getInfo(String url, String type){
        UserAgent userAgent = new UserAgent();
        try {
            userAgent.sendGET(getJsonURL(url));
        }catch (Exception e){return "Could not connect to: " + getJsonURL(url);}

        String stationName = "";
        String songInfo = "";

        String mountPoint = getMountPoint(url);

        JSONObject obj = new JSONObject(userAgent.json.toString());
        JSONArray sourceArray = obj.getJSONObject("icestats").getJSONArray("source");
        for (int i = 0; i < sourceArray.length(); i++)
        {
            if(getMountPoint(sourceArray.getJSONObject(i).getString("listenurl")).equalsIgnoreCase(mountPoint)) {
                stationName = sourceArray.getJSONObject(i).getString("server_name");
                if(sourceArray.getJSONObject(i).getString("yp_currently_playing") != null)
                    songInfo = sourceArray.getJSONObject(i).getString("yp_currently_playing");
                else
                    songInfo = sourceArray.getJSONObject(i).getString("title");
            }
        }

        if(type.equalsIgnoreCase("station"))
            return stationName;
        if(type.equalsIgnoreCase("song"))
            return songInfo;

        return "N/A";
    }

    /**
     * Gets the Json file of the radio station.
     *
     * @param url Radio station's mp3 stream.
     * @return Json file from url parameter.
     */
    private static String getJsonURL(String url){
        if(url.length() < 1)
            return "Invalid URL provided; can't get station info!";

        String[] parts = url.split("/");
        if(parts.length > 3)
            return (parts[0] + "//" + parts[2] + "/status-json.xsl");
        else
            return (parts[0] + "/status-json.xsl");
    }

    /**
     * Gets the mounting point of the radio station.
     *
     * @param url Radio station's mp3 stream.
     * @return Gets the mounting point of the url parameter.
     */
    private static String getMountPoint(String url){
        if(url.length() < 1)
            return "Invalid URL provided; can't get mount point!";

        String[] parts = url.split("/");
        if(parts.length > 3)
            return (parts[3]);
        else
            return (parts[1]);
    }
}
