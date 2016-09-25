package net.grasinga.eNetMusic;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Gets the first YouTube video from a search term.
 *
 * @see eNetMusic#getVideoId(String)
 */
class YouTubeSearch {
    private final static Charset ENCODING = StandardCharsets.UTF_8;

    /**
     * Define a global variable that identifies the name of a file that
     * contains the developer's API key.
     */
    private static final String PROPERTIES_FILENAME = "youtube.properties";

    private static final long NUMBER_OF_VIDEOS_RETURNED = 1;

    /**
     * Searches for a video on YouTube from the given term, then
     * selects the first video, and finally returns that video's id.
     *
     * @param term The search term to be used on YouTube.
     * @return The YouTube video id from the specified search term.
     */
    static String videoIdSearch(String term) {
        // Read the developer key from the properties file.
        Properties properties = new Properties();
        try {
            List<String> lines = readPropertiesFile("./" + PROPERTIES_FILENAME);
            properties.put("youtube.apikey",lines.get(0));
        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }

        try {
            YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), request -> {
            }).setApplicationName("enetmusic-youtube-search").build();

            // Prompt the user to enter a query term.
            String queryTerm = setInputQuery(term);

            // Define the API request for retrieving search results.
            YouTube.Search.List search = youtube.search().list("id,snippet");

            // Set your developer key from the Google API Console for
            // non-authenticated requests. See:
            // https://console.developers.google.com/
            String apiKey = properties.getProperty("youtube.apikey");
            search.setKey(apiKey);
            search.setQ(queryTerm);

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

            // Call the API and print results.
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            if (searchResultList != null) {
                return getVideo(searchResultList.iterator());
            }
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return "blank";
    }

    private static List<String> readPropertiesFile(String aFileName) throws IOException {
        Path path = Paths.get(aFileName);
        return Files.readAllLines(path, ENCODING);
    }

    private static String setInputQuery(String s){
        String inputQuery = "Grasinga";
        if(s.length() > 0)
            inputQuery = s;
        return inputQuery;
    }

    /**
     * Returns the video id based on Iterator.
     *
     * @param iteratorSearchResults Iterator of SearchResults to print
     * @return Video id of the first video found in a search.
     * @see YouTubeSearch#videoIdSearch(String)
     */
    private static String getVideo(Iterator<SearchResult> iteratorSearchResults) {
        if (!iteratorSearchResults.hasNext()) {
            return "blank";
        }

        SearchResult singleVideo = iteratorSearchResults.next();
        ResourceId rId = singleVideo.getId();
        return rId.getVideoId();
    }
}

