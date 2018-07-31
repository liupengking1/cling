/*
 * Copyright (C) 2013 4th Line GmbH, Switzerland
 *
 * The contents of this file are subject to the terms of either the GNU
 * Lesser General Public License Version 2 or later ("LGPL") or the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.fourthline.cling.protocol.async;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.model.message.discovery.OutgoingSearchRequest;
import org.fourthline.cling.model.message.header.MXHeader;
import org.fourthline.cling.model.message.header.STAllHeader;
import org.fourthline.cling.model.message.header.UpnpHeader;
import org.fourthline.cling.protocol.SendingAsync;
import org.fourthline.cling.transport.RouterException;

import org.fourthline.cling.model.message.UpnpOperation;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.Constants;
import org.fourthline.cling.model.UnsupportedDataException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Sending search request messages using the supplied search type.
 * <p>
 * Sends all search messages 5 times, waits 0 to 500
 * milliseconds between each sending procedure.
 * </p>
 *
 * @author Christian Bauer
 */
public class SendingSearch extends SendingAsync {

    final private static Logger log = Logger.getLogger(SendingSearch.class.getName());

    private final UpnpHeader searchTarget;
    private final int mxSeconds;

    /**
     * Defaults to {@link org.fourthline.cling.model.message.header.STAllHeader} and an MX of 3 seconds.
     */
    public SendingSearch(UpnpService upnpService) {
        this(upnpService, new STAllHeader());
    }

    /**
     * Defaults to an MX value of 3 seconds.
     */
    public SendingSearch(UpnpService upnpService, UpnpHeader searchTarget) {
        this(upnpService, searchTarget, MXHeader.DEFAULT_VALUE);
    }

    /**
     * @param mxSeconds The time in seconds a host should wait before responding.
     */
    public SendingSearch(UpnpService upnpService, UpnpHeader searchTarget, int mxSeconds) {
        super(upnpService);

        if (!UpnpHeader.Type.ST.isValidHeaderType(searchTarget.getClass())) {
            throw new IllegalArgumentException(
                    "Given search target instance is not a valid header class for type ST: " + searchTarget.getClass()
            );
        }
        this.searchTarget = searchTarget;
        this.mxSeconds = mxSeconds;
    }

    public UpnpHeader getSearchTarget() {
        return searchTarget;
    }

    public int getMxSeconds() {
        return mxSeconds;
    }

    protected void execute() throws RouterException {

        log.fine("Executing search for target: " + searchTarget.getString() + " with MX seconds: " + getMxSeconds());

        OutgoingSearchRequest msg = new OutgoingSearchRequest(searchTarget, getMxSeconds());
        prepareOutgoingSearchRequest(msg);

        for (int i = 0; i < getBulkRepeat(); i++) {
            try {

                getUpnpService().getRouter().send(msg);
                getUpnpService().getRouter().broadcast(prepareOutgoingSearchBroadcastRequest(msg), Constants.UPNP_MULTICAST_PORT);

                // UDA 1.0 is silent about this but UDA 1.1 recommends "a few hundred milliseconds"
                log.finer("Sleeping " + getBulkIntervalMilliseconds() + " milliseconds");
                Thread.sleep(getBulkIntervalMilliseconds());

            } catch (InterruptedException ex) {
                // Interruption means we stop sending search messages, e.g. on shutdown of thread pool
                break;
            }
        }
    }

    public int getBulkRepeat() {
        return 5; // UDA 1.0 says "repeat more than once"
    }

    public int getBulkIntervalMilliseconds() {
        return 500; // That should be plenty on an ethernet LAN
    }

    /**
     * Override this to edit the outgoing message, e.g. by adding headers.
     */
    protected void prepareOutgoingSearchRequest(OutgoingSearchRequest message) {
    }

    /* Prepare search broadcast message in bytes using the same msearch msg */
    protected byte[] prepareOutgoingSearchBroadcastRequest(OutgoingSearchRequest message) {
        StringBuilder statusLine = new StringBuilder();
        UpnpOperation operation = message.getOperation();
        if (operation instanceof UpnpRequest) {
            UpnpRequest requestOperation = (UpnpRequest) operation;
            statusLine.append(requestOperation.getHttpMethodName()).append(" * ");
            statusLine.append("HTTP/1.").append(operation.getHttpMinorVersion()).append("\r\n");
        } else if (operation instanceof UpnpResponse) {
            UpnpResponse responseOperation = (UpnpResponse) operation;
            statusLine.append("HTTP/1.").append(operation.getHttpMinorVersion()).append(" ");
            statusLine.append(responseOperation.getStatusCode()).append(" ").append(responseOperation.getStatusMessage());
            statusLine.append("\r\n");
        } else {
            throw new UnsupportedDataException(
                    "Message operation is not request or response, don't know how to process: " + message
            );
        }

        // UDA 1.0, 1.1.2: No body but message must have a blank line after header
        StringBuilder messageData = new StringBuilder();
        messageData.append(statusLine);
        messageData.append(message.getHeaders().toString()).append("\r\n");

        // According to HTTP 1.0 RFC, headers and their values are US-ASCII
        // TODO: Probably should look into escaping rules, too
        byte[] data;
        try {
            data = messageData.toString().getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new UnsupportedDataException(
                "Can't convert message content to US-ASCII: " + ex.getMessage(), ex, messageData
            );
        }
        return data;
    }
}
