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
package org.smallmind.quorum.transport.message.gossip;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.quorum.transport.FauxMethod;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricEvent;

public class GossipInvocationHandler implements InvocationHandler {

  private GossipTransmitter gossipTransmitter;
  private Class serviceInterface;

  public GossipInvocationHandler (GossipTransmitter gossipTransmitter, Class serviceInterface) {

    this.gossipTransmitter = gossipTransmitter;
    this.serviceInterface = serviceInterface;
  }

  public Object invoke (Object proxy, final Method method, final Object[] args)
    throws Throwable {

    InstrumentationManager.execute(new ChronometerInstrument(TransportManager.getTransport(), new MetricProperty("gossip", "true"), new MetricProperty("event", MetricEvent.INVOCATION.getDisplay()), new MetricProperty("service", serviceInterface.getSimpleName()), new MetricProperty("method", method.getName())) {

      @Override
      public void withChronometer ()
        throws Exception {

        gossipTransmitter.sendMessage(new InvocationSignal(ContextFactory.getExpectedContexts(serviceInterface), new FauxMethod(method), args), serviceInterface.getName());
      }
    });

    return null;
  }
}