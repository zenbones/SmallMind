/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.nutsnbolts.soap;

import java.io.PrintStream;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class MonitorHandler implements SOAPHandler<SOAPMessageContext> {

  private PrintStream printStream;
  private MessageDirection messageDirection;

  public MonitorHandler () {

    this(System.out, MessageDirection.BOTH);
  }

  public MonitorHandler (PrintStream printStream) {

    this(printStream, MessageDirection.BOTH);
  }

  public MonitorHandler (MessageDirection messageDirection) {

    this(System.out, messageDirection);
  }

  public MonitorHandler (PrintStream printStream, MessageDirection messageDirection) {

    this.printStream = printStream;
    this.messageDirection = messageDirection;
  }

  public Set<QName> getHeaders () {

    return null;
  }

  public boolean handleMessage (SOAPMessageContext context) {

    try {
      switch (messageDirection) {
        case IN:
          if (!(Boolean)context.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            context.getMessage().writeTo(printStream);
            printStream.println();
          }
          break;
        case OUT:
          if ((Boolean)context.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY)) {
            context.getMessage().writeTo(printStream);
            printStream.println();
          }
          break;
        case BOTH:
          context.getMessage().writeTo(printStream);
          printStream.println();
          break;
        default:
          throw new UnknownSwitchCaseException(messageDirection.name());
      }
    }
    catch (Exception exception) {
      throw new RuntimeException(exception);
    }

    return true;
  }

  public boolean handleFault (SOAPMessageContext context) {

    try {
      context.getMessage().writeTo(printStream);
      printStream.println();
    }
    catch (Exception exception) {
      throw new RuntimeException(exception);
    }

    return true;
  }

  public void close (MessageContext context) {

  }

  protected void finalize () {

    printStream.close();
  }
}