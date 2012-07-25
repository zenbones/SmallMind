/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.smallmind.instrument.Chronometer;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.MetricRegistry;
import org.smallmind.instrument.MetricRegistryFactory;
import org.smallmind.instrument.Metrics;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.quorum.transport.FauxMethod;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.Transport;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricEvent;

public class MessageInvocationHandler implements InvocationHandler {

  private static final Transport TRANSPORT;
  private static final MetricRegistry METRIC_REGISTRY;

  private MessageTransmitter messageTransmitter;
  private Class serviceInterface;

  static {

    if (((TRANSPORT = TransportManager.getTransport()) == null) || (!TRANSPORT.getMetricConfiguration().isInstrumented())) {
      METRIC_REGISTRY = null;
    }
    else {
      if ((METRIC_REGISTRY = MetricRegistryFactory.getMetricRegistry()) == null) {
        throw new ExceptionInInitializerError("No MetricRegistry instance has been registered with the MetricRegistryFactory");
      }
    }
  }

  public MessageInvocationHandler (MessageTransmitter messageTransmitter, Class serviceInterface) {

    this.messageTransmitter = messageTransmitter;
    this.serviceInterface = serviceInterface;
  }

  public Object invoke (Object proxy, Method method, Object[] args)
    throws Throwable {

    TransmissionCallback callback;
    Chronometer invocationChronometer = null;
    long invocationStart = 0;

    if (METRIC_REGISTRY != null) {
      invocationChronometer = METRIC_REGISTRY.ensure(Metrics.buildChronometer(TRANSPORT.getMetricConfiguration().getChronometerSamples(), TimeUnit.MILLISECONDS, TRANSPORT.getMetricConfiguration().getTickInterval(), TRANSPORT.getMetricConfiguration().getTickTimeUnit()), TRANSPORT.getMetricConfiguration().getMetricDomain().getDomain(), new MetricProperty("event", MetricEvent.INVOCATION.getDisplay()), new MetricProperty("service", serviceInterface.getSimpleName()), new MetricProperty("method", method.getName()));
      invocationStart = System.currentTimeMillis();
    }

    callback = messageTransmitter.sendMessage(new InvocationSignal(ContextFactory.getExpectedContexts(serviceInterface), new FauxMethod(method), args), serviceInterface.getName());

    if (METRIC_REGISTRY != null) {
      invocationChronometer.update(System.currentTimeMillis() - invocationStart, TimeUnit.MILLISECONDS);
    }

    return callback.getResult();
  }
}