package org.codehow.ical;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class Team {
    public final String id;
    public final String name;

    public final Map<String, Match> matches;

    @JsonCreator
    public Team(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("matches") Map<String, Match> matches) {
        this.id = id;
        this.name = name;
        this.matches = matches == null ? new HashMap<>() : matches;
    }

    public Team(String id, String name) {
        this(id, name, null);
    }
}
