package org.smallmind.bayeux.oumuamua.server.spi.meta;

import java.util.Arrays;
import java.util.HashSet;
import org.smallmind.bayeux.oumuamua.server.api.Protocol;
import org.smallmind.bayeux.oumuamua.server.api.Server;

public class TransportUtility {

  public static String[] accumulateSupportedTransportNames (Server<?> server) {

    HashSet<String> supportedTransportSet = new HashSet<>();

    for (String supportedProtocolName : server.getSupportedProtocolNames()) {

      Protocol supportedProtocol;

      if ((supportedProtocol = server.getSupportedProtocol(supportedProtocolName)) != null) {
        supportedTransportSet.addAll(Arrays.asList(supportedProtocol.getSupportedTransportNames()));
      }
    }

    return supportedTransportSet.toArray(new String[0]);
  }
}
