/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.bayeux.cometd.meta;

import java.util.LinkedList;
import java.util.List;

public class TransportNegotiator {

  public static String[] negotiate (String[] actualTransports, List<String> allowedTransports, String... requestedConnectionTypes)
    throws TransportNegotiationFailure {

    LinkedList<String> possibleTransports = new LinkedList<>();

    for (String actualTransport : actualTransports) {
      for (String allowedTransport : allowedTransports) {
        if (actualTransport.equals(allowedTransport)) {
          possibleTransports.add(actualTransport);
        }
      }
    }
    if (possibleTransports.isEmpty()) {

      throw new TransportNegotiationFailure("The server does not support the current transport");
    } else {

      LinkedList<String> matchingTranports = new LinkedList<>();

      for (String requestedConnectionType : requestedConnectionTypes) {
        for (String possibleTransport : possibleTransports) {
          if (possibleTransport.equalsIgnoreCase(requestedConnectionType)) {
            matchingTranports.add(possibleTransport);
          }
        }
      }

      if (matchingTranports.isEmpty()) {
        throw new TransportNegotiationFailure("Failed to negotiate the connection type");
      } else {

        return matchingTranports.toArray(new String[0]);
      }
    }
  }
}
