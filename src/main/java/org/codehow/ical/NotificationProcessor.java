package org.codehow.ical;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;

/**
 */
public class NotificationProcessor implements Processor{


    private final Notifyer[] notifyers;

    public NotificationProcessor(Notifyer[] notifyers) {
        this.notifyers = notifyers;
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        List<String> messages =
            (List<String>) exchange.getIn().getBody();

        messages.forEach(message -> Stream.of(notifyers).forEach(
            n -> {
                Response r = n.postMessage(message);
                System.out.println(format("%d: %s", r.getStatus(), r.readEntity(String.class)));
            }));
    }
}
