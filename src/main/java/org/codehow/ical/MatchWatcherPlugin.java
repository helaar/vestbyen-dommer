package org.codehow.ical;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.kantega.respiro.api.RestClientBuilder;
import org.kantega.reststop.api.Config;
import org.kantega.reststop.api.Export;
import org.kantega.reststop.api.Plugin;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;

/**
 */
@Plugin
public class MatchWatcherPlugin {

    @Export
    private final RouteBuilder homeMatcCollector;


    private final HomeMatchExtractor[] extractors = new HomeMatchExtractor[]{
        new HomeMatchExtractor("Vestbyen", "23876"),
        new HomeMatchExtractor("Vestbyen 2", "24350"),
        new HomeMatchExtractor("Vestbyen", "24353"),
        new HomeMatchExtractor("Vestbyen 2", "24659"),
        new HomeMatchExtractor("Vestbyen", "24548"),
        new HomeMatchExtractor("Vestbyen 2", "197765"),
        new HomeMatchExtractor("Vestbyen", "24656")
    };

    public MatchWatcherPlugin(
        @Config String facebookAccessToken,
        @Config String facebookBaseUrl,
        @Config String facebookGroup,
        @Config String fotballCalendarUrl,
        @Config String slackUrl,
        @Config String slackGroup,
        @Config String databaseFile,
        @Config String matchTimer,
        @Config String tokenTimer,
        RestClientBuilder builder) throws URISyntaxException {


        final WebTarget calendarTarget = builder.client().build()
            .target(new URI(fotballCalendarUrl));

        final WebTarget facebookTarget = builder.client().build()
            .target(new URI(facebookBaseUrl))
            .queryParam("access_token", facebookAccessToken);

        final WebTarget slackTarget = builder.client().build()
            .target(new URI(slackUrl));

        final Notifyer slack = new SlackNotifyer(slackTarget, slackGroup);

        final Notifyer[] notifyers = new Notifyer[]{
            new FacebookNotifyer(facebookTarget, facebookGroup),
            slack
        };

        final File database = new File(databaseFile);


        final Processor tokenChecker = exchange -> {

            final Response resp = facebookTarget.path("me").request().get();
            if (resp.getStatus() != 200)
                slack.postMessage(format("TOKEN: %d %s\n - %s",
                    resp.getStatus(), "```"+resp.readEntity(String.class).replaceAll("\"","'")+"```",
                    "https://developers.facebook.com/apps/180021062790223/dashboard/"));
        };

        homeMatcCollector = new RouteBuilder() {
            @Override
            public void configure()  {
                from(matchTimer)
                    .process(new HomeMatchProcessor(extractors, calendarTarget, database))
                    .process(new NotificationProcessor(notifyers));

                from(tokenTimer)
                    .process(tokenChecker);
            }
        };

    }
}
