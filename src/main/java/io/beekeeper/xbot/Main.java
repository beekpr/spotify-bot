package io.beekeeper.xbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Configuration config = null;
        try {
            config = mapper.readValue(new File("config/configuration.yaml"), Configuration.class);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        new SpotifyToYoutubeBot(config).start();
    }

}
