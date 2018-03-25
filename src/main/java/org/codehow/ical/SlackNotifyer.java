package org.codehow.ical;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 */
public class SlackNotifyer implements Notifyer {

    private final WebTarget slackTarget;

    private final String channel;

    private final static String PAYLOAD_FORMAT = "{" +
        "  \"text\" : \"%s\"," +
        "  \"username\" : \"KampBot\"," +
        "  \"icon_emoji\" : \":soccer:\"," +
        "  \"channel\" : \"%s\"" +
        "}";

    public SlackNotifyer(WebTarget slackTarget, String channel) {
        this.slackTarget = slackTarget;
        this.channel = channel;
    }

    @Override
    public Response postMessage( String message) {


        return slackTarget.request().post(Entity.json(String.format(PAYLOAD_FORMAT, message, channel)));
    }


}
