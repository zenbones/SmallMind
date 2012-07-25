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

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import javax.jms.Message;
import javax.jms.Session;
import org.smallmind.instrument.Chronometer;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.MetricRegistry;
import org.smallmind.instrument.MetricRegistryFactory;
import org.smallmind.instrument.Metrics;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.MethodInvoker;
import org.smallmind.quorum.transport.Transport;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricEvent;

public class InvocationMessageTarget implements MessageTarget {

  private static final Transport TRANSPORT;
  private static final MetricRegistry METRIC_REGISTRY;

  private MethodInvoker methodInvoker;
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

  public InvocationMessageTarget (Object targetObject, Class serviceInterface)
    throws NoSuchMethodException {

    methodInvoker = new MethodInvoker(targetObject, new Class[] {this.serviceInterface = serviceInterface});
  }

  @Override
  public Class getServiceInterface () {

    return serviceInterface;
  }

  @Override
  public Message handleMessage (Session session, MessageStrategy messageStrategy, Message message)
    throws Exception {

    Message responseMessage;
    InvocationSignal invocationSignal;
    Serializable result;
    Chronometer invocationChronometer = null;
    Chronometer serializationChronometer = null;
    long invocationStart = 0;
    long serializationStart = 0;

    invocationSignal = (InvocationSignal)messageStrategy.unwrapFromMessage(message);

    if (METRIC_REGISTRY != null) {
      invocationChronometer = METRIC_REGISTRY.ensure(Metrics.buildChronometer(TRANSPORT.getMetricConfiguration().getChronometerSamples(), TimeUnit.MILLISECONDS, TRANSPORT.getMetricConfiguration().getTickInterval(), TRANSPORT.getMetricConfiguration().getTickTimeUnit()), TRANSPORT.getMetricConfiguration().getMetricDomain().getDomain(), new MetricProperty("event", MetricEvent.INVOCATION.getDisplay()), new MetricProperty("service", serviceInterface.getSimpleName()), new MetricProperty("method", invocationSignal.getFauxMethod().getName()));
      invocationStart = System.currentTimeMillis();
    }

    result = (Serializable)methodInvoker.remoteInvocation(invocationSignal);

    if (METRIC_REGISTRY != null) {
      invocationChronometer.update(System.currentTimeMillis() - invocationStart, TimeUnit.MILLISECONDS);

      serializationChronometer = METRIC_REGISTRY.ensure(Metrics.buildChronometer(TRANSPORT.getMetricConfiguration().getChronometerSamples(), TimeUnit.MILLISECONDS, TRANSPORT.getMetricConfiguration().getTickInterval(), TRANSPORT.getMetricConfiguration().getTickTimeUnit()), TRANSPORT.getMetricConfiguration().getMetricDomain().getDomain(), new MetricProperty("event", MetricEvent.CONSTRUCT_MESSAGE.getDisplay()));
      serializationStart = System.currentTimeMillis();
    }

    responseMessage = messageStrategy.wrapInMessage(session, result);

    if (METRIC_REGISTRY != null) {
      serializationChronometer.update(System.currentTimeMillis() - serializationStart, TimeUnit.MILLISECONDS);
    }

    return responseMessage;
  }
}
