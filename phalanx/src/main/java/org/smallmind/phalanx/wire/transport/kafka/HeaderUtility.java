package org.smallmind.phalanx.wire.transport.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

public class HeaderUtility {

  public static final String MESSAGE_ID = "messageId";
  public static final String CALLER_ID = "callerId";
  public static final String CORRELATION_ID = "correlationId";

  public static String getHeader (ConsumerRecord<Long, byte[]> record, String headerName) {

    for (Header header : record.headers()) {
      if (header.key().equals(headerName)) {
        return new String(header.value());
      }
    }

    return null;
  }
}
