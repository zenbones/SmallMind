/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.transport.message;

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import javax.jms.Message;
import javax.jms.Session;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.MethodInvoker;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricEvent;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

public class InvocationMessageTarget implements MessageTarget {

  private MethodInvoker methodInvoker;
  private Class serviceInterface;
  private Level logLevel = Level.DEBUG;

  public InvocationMessageTarget (Object targetObject, Class serviceInterface)
    throws NoSuchMethodException {

    methodInvoker = new MethodInvoker(targetObject, new Class[] {this.serviceInterface = serviceInterface});
  }

  public InvocationMessageTarget setLogLevel (Level logLevel) {

    this.logLevel = logLevel;

    return this;
  }

  @Override
  public Class getServiceInterface () {

    return serviceInterface;
  }

  @Override
  public Message handleMessage (final Session session, final MessageStrategy messageStrategy, Message message)
    throws Exception {

    final Serializable result;
    final InvocationSignal invocationSignal = (InvocationSignal)messageStrategy.unwrapFromMessage(message);
    final long startTime = System.currentTimeMillis();

    try {

      long totalTime;

      result = (Serializable)methodInvoker.remoteInvocation(invocationSignal);

      InstrumentationManager.instrumentWithChronometer(TransportManager.getTransport(), totalTime = System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS, new MetricProperty("event", MetricEvent.INVOCATION.getDisplay()), new MetricProperty("service", serviceInterface.getSimpleName()), new MetricProperty("method", invocationSignal.getFauxMethod().getName()));
      LoggerManager.getLogger(InvocationMessageTarget.class).log(logLevel, "%s.%s() %d ms", serviceInterface.getSimpleName(), invocationSignal.getFauxMethod().getName(), totalTime);
    }
    catch (Exception exception) {
      LoggerManager.getLogger(InvocationMessageTarget.class).log(logLevel, "%s.%s() %d ms - %s", serviceInterface.getSimpleName(), invocationSignal.getFauxMethod().getName(), System.currentTimeMillis() - startTime, exception.getMessage());

      throw exception;
    }

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<Message>(TransportManager.getTransport(), new MetricProperty("event", MetricEvent.CONSTRUCT_MESSAGE.getDisplay())) {

      @Override
      public Message withChronometer ()
        throws Exception {

        //TODO: Debug, to be removed when serialization failure solved
        try {

          return messageStrategy.wrapInMessage(session, result);
        }
        catch (Exception exception) {
          if (exception.getMessage().equals("Failed to serialize object")) {

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

            try {
              objectOutputStream.writeObject(result);
            }
            catch (NotSerializableException n) {
              throw new FormattedRuntimeException("No serializable class(%s) in return type(%s) from service(%s)", n.getMessage(), result.getClass().isArray() ? result.getClass().getComponentType().getName() + "[]" : result.getClass().getName(), serviceInterface.getSimpleName() + "." + invocationSignal.getFauxMethod().getName() + "()");
            }

            objectOutputStream.close();
            byteArrayOutputStream.close();

            throw new FormattedRuntimeException("HornetQ doesn't like to serialize I guess...");
          }

          throw exception;
        }
      }
    });
  }
}
