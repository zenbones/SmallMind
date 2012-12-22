package org.smallmind.websocket;

import java.util.HashSet;

public class ProtocolValidator {

  private static final String SEPARATORS = "()<>@,;:\\\"/[]?={} \t";

  public static boolean validate (String... protocols) {

    if ((protocols != null) && (protocols.length > 0)) {

      HashSet<String> protocolSet = new HashSet<>();

      for (String protocol : protocols) {
        for (char singleChar : protocol.toCharArray()) {
          if (!((SEPARATORS.indexOf(singleChar) >= 0) || ((singleChar >= 33) && (singleChar <= 126)))) {

            return false;
          }
        }

        if (!protocolSet.add(protocol)) {

          return false;
        }
      }
    }

    return true;
  }
}
