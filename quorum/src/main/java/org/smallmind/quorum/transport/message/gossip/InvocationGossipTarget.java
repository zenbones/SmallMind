/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.transport.message.gossip;

import javax.jms.Message;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.MethodInvoker;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricInteraction;
import org.smallmind.quorum.transport.message.MessageStrategy;

public class InvocationGossipTarget implements GossipTarget {

  private MethodInvoker methodInvoker;
  private Class serviceInterface;

  public InvocationGossipTarget (Object targetObject, Class serviceInterface)
    throws NoSuchMethodException {

    methodInvoker = new MethodInvoker(targetObject, new Class[]{this.serviceInterface = serviceInterface});
  }

  @Override
  public Class getServiceInterface () {

    return serviceInterface;
  }

  @Override
  public void handleMessage (final MessageStrategy messageStrategy, Message message)
    throws Exception {

    final InvocationSignal invocationSignal;

    invocationSignal = (InvocationSignal)messageStrategy.unwrapFromMessage(message);

    InstrumentationManager.execute(new ChronometerInstrument(TransportManager.getTransport(), new MetricProperty("gossip", "true"), new MetricProperty("event", MetricInteraction.INVOCATION.getDisplay()), new MetricProperty("service", serviceInterface.getSimpleName()), new MetricProperty("method", invocationSignal.getFauxMethod().getName())) {

      @Override
      public void withChronometer ()
        throws Exception {

        methodInvoker.remoteInvocation(invocationSignal);
      }
    });
  }
}
