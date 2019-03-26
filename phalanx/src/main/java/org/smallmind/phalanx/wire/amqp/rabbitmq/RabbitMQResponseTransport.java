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
package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.ResponseTransport;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.TransportState;
import org.smallmind.phalanx.wire.WireInvocationCircuit;
import org.smallmind.phalanx.wire.WiredService;
import org.smallmind.phalanx.worker.WorkManager;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.WorkerFactory;

public class RabbitMQResponseTransport extends WorkManager<InvocationWorker, RabbitMQMessage> implements WorkerFactory<InvocationWorker, RabbitMQMessage>, ResponseTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicReference<TransportState> transportStateRef = new AtomicReference<>(TransportState.PLAYING);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final SignalCodec signalCodec;
  private final ConcurrentLinkedQueue<ResponseMessageRouter> responseQueue;
  private final ResponseMessageRouter[] responseMessageRouters;
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();

  public RabbitMQResponseTransport (MetricConfiguration metricConfiguration, RabbitMQConnector rabbitMQConnector, NameConfiguration nameConfiguration, Class<InvocationWorker> workerClass, SignalCodec signalCodec, String serviceGroup, int clusterSize, int concurrencyLimit, int messageTTLSeconds, boolean autoAcknowledge)
    throws IOException, InterruptedException, TimeoutException {

    super(metricConfiguration, workerClass, concurrencyLimit);

    int routerIndex = 0;

    this.signalCodec = signalCodec;

    responseMessageRouters = new ResponseMessageRouter[clusterSize];
    for (int index = 0; index < responseMessageRouters.length; index++) {
      responseMessageRouters[index] = new ResponseMessageRouter(rabbitMQConnector, nameConfiguration, this, signalCodec, serviceGroup, instanceId, index, messageTTLSeconds, autoAcknowledge);
      responseMessageRouters[index].initialize();
    }

    responseQueue = new ConcurrentLinkedQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      responseQueue.add(responseMessageRouters[routerIndex]);
      if (++routerIndex == responseMessageRouters.length) {
        routerIndex = 0;
      }
    }

    startUp(this);
  }

  @Override
  public String getInstanceId () {

    return instanceId;
  }

  @Override
  public String register (Class<?> serviceInterface, WiredService targetService) throws Exception {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  @Override
  public InvocationWorker createWorker (MetricConfiguration metricConfiguration, WorkQueue<RabbitMQMessage> transferQueue) {

    return new InvocationWorker(metricConfiguration, transferQueue, this, invocationCircuit, signalCodec);
  }

  @Override
  public TransportState getState () {

    return transportStateRef.get();
  }

  @Override
  public void play ()
    throws Exception {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PAUSED, TransportState.PLAYING)) {
        for (ResponseMessageRouter responseMessageRouter : responseMessageRouters) {
          responseMessageRouter.play();
        }
      }
    }
  }

  @Override
  public void pause ()
    throws Exception {

    synchronized (transportStateRef) {
      if (transportStateRef.compareAndSet(TransportState.PLAYING, TransportState.PAUSED)) {
        for (ResponseMessageRouter responseMessageRouter : responseMessageRouters) {
          responseMessageRouter.pause();
        }
      }
    }
  }

  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result) throws Throwable {

    ResponseMessageRouter responseMessageRouter;

    if ((responseMessageRouter = responseQueue.poll()) == null) {
      throw new TransportException("Unable to take a ResponseMessageRouter, which should never happen - please contact your system administrator");
    }

    responseMessageRouter.publish(callerId, correlationId, error, nativeType, result);
    responseQueue.add(responseMessageRouter);
  }

  @Override
  public void close ()
    throws IOException, InterruptedException, TimeoutException {

    if (closed.compareAndSet(false, true)) {
      synchronized (transportStateRef) {
        transportStateRef.set(TransportState.CLOSED);

        for (ResponseMessageRouter responseMessageRouter : responseMessageRouters) {
          responseMessageRouter.close();
        }

        shutDown();
      }
    }
  }
}
