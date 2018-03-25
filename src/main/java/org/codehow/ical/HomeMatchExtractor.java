package org.codehow.ical;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class HomeMatchExtractor {

    enum Property {
        SUMMARY, DTSTART, LOCATION, URL, unknown;

        static Property fromString(String s) {
            try {
                return valueOf(s);
            } catch (IllegalArgumentException e) {
                return unknown;
            }
        }
    }


    private final Pattern nameRegexp = Pattern.compile("(?s)BEGIN:VCALENDAR.*PRODID:(.*?)[\\n]");
    private final Pattern eventRegexp = Pattern.compile("(?s)BEGIN:VEVENT(.*?)END:VEVENT");
    private final Pattern propertyRegexp = Pattern.compile("(?s)(.*?)[:](.*?)\\n");
    private final Pattern homeRegexp;

    private final String teamId;

    private final static SimpleDateFormat src = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    private final static SimpleDateFormat target = new SimpleDateFormat("d.M.yyyy HH:mm");

    public HomeMatchExtractor(String homeName, String teamId) {
        homeRegexp = Pattern.compile(homeName + ".*?-");
        this.teamId = teamId;
    }

    public Team getMatches(WebTarget getCalendarTarget) {
        final Response resp = getCalendarTarget
            .queryParam("teamId", teamId)
            .request().get();

        if (resp.getStatus() == 200)
            return extractMatches(resp.readEntity(String.class));

        else
            throw new RuntimeException(resp.getStatus() + ": " + resp.getStatusInfo().getReasonPhrase());
    }

    private Team extractMatches(String iCal) {
        final String uniform = iCal
            .replaceAll("\\r", "\n")
            .replaceAll("\\n\\n", "\n")
            .replaceAll("DTSTART;", "DTSTART:");

        final Matcher nameMatch = nameRegexp.matcher(uniform);
        if (!nameMatch.find())
            throw new IllegalArgumentException("Name not found in calendar");
        final Team home = new Team(teamId, nameMatch.group(1));

        final Matcher eventMatch = eventRegexp.matcher(uniform);
        while (eventMatch.find()) {
            final Match m = new Match();
            final Matcher propMatch = propertyRegexp.matcher(eventMatch.group(1));
            while (propMatch.find()) {
                final Property prop = Property.fromString(propMatch.group(1));
                switch (prop) {
                    case DTSTART:
                        try {
                            m.start = target.format(src.parse(propMatch.group(2).split(":")[1]));
                        } catch( Throwable e) {
                            m.start = propMatch.group(2);
                        }

                        break;
                    case SUMMARY:
                        m.match = propMatch.group(2);
                        break;
                    case LOCATION:
                        m.location = propMatch.group(2);
                        break;
                    case URL:
                        m.url = propMatch.group(2);
                        m.uid = m.url.split("fiksId=")[1].trim();
                        break;

                }
            }

            final Matcher homeMatcher = homeRegexp.matcher(m.match);
            if (homeMatcher.find())
                home.matches.put(m.uid, m);
        }
        return home;
    }
}
