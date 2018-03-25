package org.codehow.ical;

import static java.lang.String.format;

/**
 */
public class Match {
    public String uid;
    public String match;
    public String start;
    public String location;
    public String url;

    @Override
    public String toString() {
        return format("%s: %s (%s) ", match, start, location );
    }
}
