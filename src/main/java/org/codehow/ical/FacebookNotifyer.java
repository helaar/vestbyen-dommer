package org.codehow.ical;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 */
public class FacebookNotifyer implements Notifyer {

    private final WebTarget graphTarget;

    private final String groupId;

    public FacebookNotifyer(WebTarget graphTarget, String groupId) {
        this.graphTarget = graphTarget;
        this.groupId = groupId;
    }


    public Response postMessage(String message) {

        return graphTarget.path(groupId).path("feed").queryParam("message", message)
            .request().post(Entity.text(""));
    }
}
