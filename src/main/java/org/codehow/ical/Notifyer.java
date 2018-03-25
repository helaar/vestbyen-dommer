package org.codehow.ical;

import javax.ws.rs.core.Response;

/**
 */
public interface Notifyer {

    Response postMessage(String message);
}
