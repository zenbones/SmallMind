/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.phalanx.wire.transport;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.phalanx.wire.ArgumentRectifier;
import org.smallmind.phalanx.wire.Methodology;
import org.smallmind.phalanx.wire.MismatchedArgumentException;
import org.smallmind.phalanx.wire.MissingInvocationException;
import org.smallmind.phalanx.wire.ServiceDefinitionException;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.signal.Function;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.smallmind.web.json.scaffold.fault.FaultElement;
import org.smallmind.web.json.scaffold.fault.FaultWrappingException;

public class WireInvocationCircuit {

  private final ConcurrentHashMap<ServiceKey, MethodInvoker> invokerMap = new ConcurrentHashMap<>();

  public void register (Class<?> serviceInterface, WiredService targetService)
    throws NoSuchMethodException, ServiceDefinitionException {

    invokerMap.putIfAbsent(new ServiceKey(targetService.getVersion(), targetService.getServiceName()), new MethodInvoker(targetService, serviceInterface));
  }

  public void handle (ResponseTransmitter transmitter, SignalCodec signalCodec, String callerId, String messageId, InvocationSignal invocationSignal) {

    MethodInvoker methodInvoker;
    Object result = null;
    String nativeType = null;
    boolean error = false;

    try {
      try {

        Function invocationFunction;
        Methodology methodology;
        Object[] arguments;

        if ((methodInvoker = invokerMap.get(new ServiceKey(invocationSignal.getRoute().getVersion(), invocationSignal.getRoute().getService()))) == null) {
          throw new ServiceDefinitionException("Unregistered service(version = %d, name = %s)", invocationSignal.getRoute().getVersion(), invocationSignal.getRoute().getService());
        }
        if ((invocationFunction = invocationSignal.getRoute().getFunction()).isPartial()) {

          Function completeFunction;

          if ((completeFunction = methodInvoker.match(invocationFunction)) == null) {
            throw new MissingInvocationException("Unable to locate the proper method for the partial function(%s) of service(%s)", invocationFunction.getName(), invocationSignal.getRoute().getService());
          }

          invocationFunction = completeFunction;
        }

        methodology = methodInvoker.getMethodology(invocationFunction);
        arguments = ArgumentRectifier.constructArray(signalCodec,invocationSignal,invocationFunction, methodology);
        nativeType = invocationFunction.getNativeType();

        result = methodInvoker.remoteInvocation(invocationSignal.getContexts(), invocationFunction, arguments);

        if ((result != null) && (!(result instanceof Serializable))) {
          throw new TransportException("The result(%s) of this call is not Serializable", result.getClass().getName());
        }
      } catch (Exception exception) {
        if (!invocationSignal.isInOnly()) {
          error = true;
          result = (exception instanceof FaultWrappingException) ? ((FaultWrappingException)exception).getFault() : new Fault(new FaultElement(invocationSignal.getRoute().getService(), invocationSignal.getRoute().getFunction().getName()), exception);
        }
      }

      if (!invocationSignal.isInOnly()) {
        transmitter.transmit(callerId, messageId, error, nativeType, result);
      }
    } catch (Throwable throwable) {
      LoggerManager.getLogger(WireInvocationCircuit.class).error(throwable);
    }
  }

  private static class ServiceKey {

    private final String service;
    private final int version;

    private ServiceKey (int version, String service) {

      this.version = version;
      this.service = service;
    }

    public String getService () {

      return service;
    }

    public int getVersion () {

      return version;
    }

    @Override
    public int hashCode () {

      return service.hashCode() ^ version;
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ServiceKey) && ((ServiceKey)obj).getService().equals(service) && (((ServiceKey)obj).getVersion() == version);
    }
  }
}
