package io.beekeeper.xbot;

import lombok.Getter;

@Getter
public class Configuration {
    private String appName;

    private String tenantUrl;
    private String apiToken;
    private String botName;
    private String botNameRegexp;

    private String spotifyClientId;
    private String spotifyClientSecret;

    private String youtubeApiKey;
}
