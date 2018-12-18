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
        new HomeMatchExtractor("Vestbyen", "23876"), // G11-1
        new HomeMatchExtractor("Vestbyen 2", "24350"), // G11-2
        new HomeMatchExtractor("Vestbyen 2", "24659"), // G12-2
        new HomeMatchExtractor("Vestbyen", "24548"), // J11-1
        new HomeMatchExtractor("Vestbyen 2", "197765"), // J11-2
        new HomeMatchExtractor("Vestbyen", "34562"), // G13-1
        new HomeMatchExtractor("Vestbyen 2", "198284") // J14-2
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
            //new FacebookNotifyer(facebookTarget, facebookGroup),
            slack
        };

        final File database = new File(databaseFile);


        final Processor tokenChecker = exchange -> {

            final Response resp = facebookTarget.path("me").request().get();
            if (resp.getStatus() != 200)
                slack.postMessage(format(":warning: TOKEN: %d %s\n - %s",
                    resp.getStatus(), "```" + resp.readEntity(String.class).replaceAll("\"", "'") + "```",
                    "https://developers.facebook.com/apps/180021062790223/dashboard/"));
        };

        homeMatcCollector = new RouteBuilder() {
            @Override
            public void configure() {
                onException(Throwable.class)
                    .process(
                        e -> slack.postMessage(":bangbang: " + e.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class).getMessage())
                    );

                from(matchTimer)
                    .process(new HomeMatchProcessor(extractors, calendarTarget, builder.client().build(), database))
                    .process(new NotificationProcessor(notifyers));

                // Kjører ikke mot facebook lenger, trenger ikke å sjekke
                // from(tokenTimer)
                //    .process(tokenChecker);
            }
        };

        slack.postMessage(":exclamation: Tjenesten restartet. Poster til https://www.facebook.com/groups/" + facebookGroup);

    }
}
