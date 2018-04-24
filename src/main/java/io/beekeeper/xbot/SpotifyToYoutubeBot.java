package io.beekeeper.xbot;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Track;
import io.beekeeper.sdk.ChatBot;
import io.beekeeper.sdk.exception.BeekeeperException;
import io.beekeeper.sdk.model.ConversationMember;
import io.beekeeper.sdk.model.ConversationMessage;
import io.beekeeper.sdk.model.ConversationType;
import io.beekeeper.sdk.model.MessageType;
import io.beekeeper.sdk.params.InputPromptOptionParams;
import io.beekeeper.sdk.params.InputPromptParams;
import io.beekeeper.sdk.params.SendMessageParams;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SpotifyToYoutubeBot extends ChatBot{

    Configuration config;

    String spotifyLinkRegexp = ".*open\\.spotify\\.com.*";

    SpotifyApi spotifyApi;
    YouTube youtubeApi;


    YouTube.Search.List youtubeSearch;

    String spotifyTrackIdRegexp = ".*open\\.spotify\\.com/track/([a-zA-Z0-9]+)\\?si=([a-zA-Z0-9\\-])*";

    Pattern spotifyTrackIdPattern;


    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    private static HttpRequestInitializer HTTP_REQUEST_INITIALIZER = new HttpRequestInitializer() {
        @Override
        public void initialize(HttpRequest request) throws IOException {

        }
    };

    public SpotifyToYoutubeBot(Configuration config) {
        super(config.getTenantUrl(), config.getApiToken());
        this.config = config;

        spotifyTrackIdPattern = Pattern.compile(spotifyTrackIdRegexp);

        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(config.getSpotifyClientId())
                .setClientSecret(config.getSpotifyClientSecret())
                .build();

        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            this.youtubeApi = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, HTTP_REQUEST_INITIALIZER)
                    .setYouTubeRequestInitializer(new YouTubeRequestInitializer(config.getYoutubeApiKey()))
                    .setApplicationName(config.getAppName())
                    .build();

            youtubeSearch =  youtubeApi.search().list("id,snippet");
            youtubeSearch.setType("video");
            youtubeSearch.setKey(config.getYoutubeApiKey());
            youtubeSearch.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            youtubeSearch.setMaxResults(1L);
        }
        catch (Exception e) {
            // TODO this not nice
            log.error("whee", e);
            throw new RuntimeException();
        }

        log.info("Bot started");

    }

    private void authorizeWithSpotify() {
        try {
            ClientCredentials creds = spotifyApi.clientCredentials().build().execute();
            log.info("Got new Spotify access token: {}", creds.getAccessToken());
            spotifyApi.setAccessToken(creds.getAccessToken());
        }
        catch (IOException | SpotifyWebApiException e) {
            log.error("Could not authorize with Spotify", e);
        }
    }

    @Override
    public void onNewMessage(ConversationMessage message, ConversationHelper conversationHelper) {
        if (message.getText() == null) {
            return;
        }
        try {
            boolean isOneOnOne = getSdk().getConversations().getConversationById(message.getConversationId()).execute().getType() == ConversationType.ONE_ON_ONE;
            if (message.getText().toLowerCase().matches(addPrefixToRegexp("help.*", isOneOnOne))) {
                log.debug("Sending help text");
                conversationHelper.reply("Hello! I am " + config.getBotName() + ".\n\n" +
                        "I'm here to listen for spotify links and find matching youtube videos, so that all people can enjoy the wonderful music you're posting. " +
                        "Just add me to a chat and I'll do my work!");
            }
            if (message.getText().toLowerCase().matches(spotifyLinkRegexp)) {
                log.info(message.getText());

                authorizeWithSpotify();

                Matcher trackIdMatcher = spotifyTrackIdPattern.matcher(message.getText());

                if (trackIdMatcher.find()) {
                    log.info(trackIdMatcher.group(1));
                    String trackId = trackIdMatcher.group(1);

                    Track track = spotifyApi.getTrack(trackId).build().execute();


                    youtubeSearch.setQ(track.getName() + " " + track.getArtists()[0].getName());

                    String friendlyText = "I detected a spotify link for " + track.getArtists()[0].getName() + " - "
                            + track.getName() + ". I tried to find a matching youtube video: \n";

                    SearchListResponse searchListResponse = youtubeSearch.execute();

                    Iterator<SearchResult> iterator = searchListResponse.getItems().iterator();
                    if (iterator.hasNext()) {
                        SearchResult result = iterator.next();
                        String url = "youtube.com/watch?v=" + result.getId().getVideoId();
                        conversationHelper.reply(friendlyText + url);
                    }

                    else {
                        log.info("No search result");
                    }



                }
            }
        } catch (BeekeeperException e) {
            // Failed to reply to the message
            log.error("Exception while talking to Beekeeper API.", e);
        }
        catch (SpotifyWebApiException e) {
            // Failed to reply to the message
            log.error("Exception while talking to Spotify API.", e);
        }
        catch (IOException e) {
            // Failed to reply to the message
            log.error("IOException while talking to Spotify API.", e);
        }
    }

    private String addPrefixToRegexp(String regexp, boolean isOneOnOne) {
        return "^"
                + (isOneOnOne ? "(" : "")
                + config.getBotNameRegexp() + "[,:]?\\s*"
                + (isOneOnOne ? ")?" : "")
                + regexp;
    }

    private String getConversationNameFromMessage(ConversationMessage message) throws BeekeeperException {
        return getSdk().getConversations().getConversationById(message.getConversationId()).execute().getName();
    }

}
