package org.codehow.ical;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import javax.ws.rs.client.WebTarget;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Optional.empty;

/**
 */
public class HomeMatchProcessor implements Processor {

    private final HomeMatchExtractor[] extractors;
    private final WebTarget target;
    private final ObjectMapper om = new ObjectMapper();

    private final File database;


    public HomeMatchProcessor(HomeMatchExtractor[] extractors, WebTarget target, File database) {
        this.extractors = extractors;
        this.target = target;
        this.database = database;
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        final Map<String, Team> teamMap =
            Stream.of(extractors)
                .map(e -> e.getMatches(target))
                .collect(Collectors.toMap(match -> match.id, match -> match));

        final Map<String, Team> storedMap = database.exists() && database.isFile()
            ? om.readValue(database, new TypeReference<HashMap<String, Team>>() {
        })
            : new HashMap();

        final ArrayList<String> messages = new ArrayList<>();

        storedMap.keySet().stream()
            .filter(id -> !teamMap.containsKey(id))
            .map(storedMap::get)
            .forEach(team -> messages.add(
                format("Team %s (%s) fjernet fra databasen", team.id, team.name)
            ));

        if (teamMap.size() > 0 && storedMap.size() > 0)
            teamMap.values().forEach(
                team -> detectChanges(team, storedMap.get(team.id), messages)
            );

        om.writeValue(database, teamMap);

        exchange.getIn().setBody(messages);
    }

    private void detectChanges(Team newTeam, Team oldTeam, ArrayList<String> messages) {

        if( oldTeam == null)
            messages.add(format("Team %s (%s) lagt til i databasen", newTeam.id, newTeam.name));
        else {

            newTeam.matches.values()
                .forEach(
                    match -> detectCanges(newTeam, match, oldTeam.matches.get(match.uid), messages)
                );

            oldTeam.matches.values().stream()
                .filter(match -> !newTeam.matches.containsKey(match.uid))
                .forEach(match -> messages.add(format(
                    "Fjernet kamp for %s (%s): \n  %s\n  %s",
                    newTeam.id, newTeam.name, match.url, match.toString()
                    ))
                );
        }
    }

    private void detectCanges(Team newTeam, Match newMatch, Match oldMatch, ArrayList<String> messages) {
        if (oldMatch == null)
            messages.add(format(
                "Ny kamp for %s (%s): \n  %s\n  %s",
                newTeam.id, newTeam.name, newMatch.url, newMatch.toString()
            ));
        else {
            final List<String> changes = Stream.of(
                compare("*BANE*", newMatch.location, oldMatch.location),
                compare("*KAMP*", newMatch.match, oldMatch.match),
                compare("*TIDSPUNKT*", newMatch.start, oldMatch.start))
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toList());

            if (!changes.isEmpty()) {
                messages.add(format(
                    "Endringer i kamp for %s (%s) \n  %s\n  %s",
                    newTeam.name, newTeam.id, newMatch.url, join("\n  ", changes)
                ));
            }

        }

    }

    private static Optional<String> compare(String label, String newS, String oldS) {
        if (!newS.equals(oldS))
            return Optional.of(format("%s er endret fra '%s' til '%s'", label, oldS, newS));
        else
            return empty();
    }

}
