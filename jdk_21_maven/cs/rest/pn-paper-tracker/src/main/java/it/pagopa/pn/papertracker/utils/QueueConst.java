package it.pagopa.pn.papertracker.utils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class QueueConst {
    public static final String PUBLISHER = "pn-paper-tracker";
    public static final String OCR_REQUEST_EVENT_TYPE = "OCR_REQUEST";
    public static final String DELIVERY_PUSH_EVENT_TYPE = "SEND_ANALOG_RESPONSE";
    public static final String TRACKER_QUEUE_PROXY_EVENT_TYPE = "QUEUE_PROXY";
}
