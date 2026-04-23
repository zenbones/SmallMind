/*
 * Copyright (c) 2007 through 2026 David Berkman
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
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.phalanx.wire.ArgumentRectifier;
import org.smallmind.phalanx.wire.Methodology;
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

/**
 * Routes incoming wire invocation signals to registered service implementations and transmits results back to callers.
 *
 * <p>Services are registered by interface and version; each incoming {@link InvocationSignal} is matched
 * against the registry, the target method is invoked reflectively, and the outcome (or a fault on error)
 * is forwarded through the supplied {@link ResponseTransmitter}.  Fire-and-forget (in-only) invocations
 * suppress response transmission and log errors instead.</p>
 */
public class WireInvocationCircuit {

  private final ConcurrentHashMap<ServiceKey, MethodInvoker> invokerMap = new ConcurrentHashMap<>();

  /**
   * Registers a service implementation so that incoming invocation signals targeting its interface can be dispatched.
   *
   * <p>If a service with the same version and name is already registered the call is a no-op.</p>
   *
   * @param serviceInterface the interface whose methods are exposed remotely
   * @param targetService    wrapper that carries the implementing object, service name, and version
   * @throws NoSuchMethodException      if reflective method lookup against {@code serviceInterface} fails
   * @throws ServiceDefinitionException if required annotation metadata on {@code serviceInterface} is missing or malformed
   */
  public void register (Class<?> serviceInterface, WiredService targetService)
    throws NoSuchMethodException, ServiceDefinitionException {

    invokerMap.putIfAbsent(new ServiceKey(targetService.getVersion(), targetService.getServiceName()), new MethodInvoker(targetService, serviceInterface));
  }

  /**
   * Dispatches an invocation signal to the appropriate service method and transmits the result.
   *
   * <p>Partial function descriptors are resolved to their complete form before invocation.  On success
   * the return value is sent via {@code transmitter}; on failure a {@link Fault} is sent instead.
   * For in-only invocations errors are only logged and no response is transmitted.  Any throwable
   * escaping the transmission step is caught and logged to prevent thread termination.</p>
   *
   * @param transmitter      transmitter through which the response or fault is sent to the caller
   * @param signalCodec      codec used to deserialise invocation arguments from the signal payload
   * @param callerId         transport-level identifier of the originating caller
   * @param messageId        correlation identifier used to match this response to the original request
   * @param invocationSignal the incoming signal describing the service, function, arguments, and contexts
   */
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
        arguments = ArgumentRectifier.constructArray(signalCodec, invocationSignal, invocationFunction, methodology);
        nativeType = invocationFunction.getNativeType();

        result = methodInvoker.remoteInvocation(invocationSignal.getContexts(), invocationFunction, arguments);

        if ((result != null) && (!(result instanceof Serializable))) {
          throw new TransportException("The result(%s) of this call is not Serializable", result.getClass().getName());
        }
      } catch (Exception exception) {
        if (invocationSignal.isInOnly()) {
          LoggerManager.getLogger(WireInvocationCircuit.class).error(exception);
        } else {
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

  /**
   * Composite map key that identifies a registered service by its numeric version and logical name.
   *
   * @param version integer version discriminator for the service
   * @param service logical service name
   */
  private record ServiceKey(int version, String service) {

    private ServiceKey {

    }

    /**
     * Returns the logical service name component of this key.
     *
     * @return service name
     */
    @Override
    public String service () {

      return service;
    }

    /**
     * Returns the version component of this key.
     *
     * @return service version
     */
    @Override
    public int version () {

      return version;
    }

    /**
     * Returns a hash code derived from both the service name and version.
     *
     * @return combined hash code
     */
    @Override
    public int hashCode () {

      return service.hashCode() ^ version;
    }

    /**
     * Returns {@code true} when {@code obj} is a {@code ServiceKey} with the same service name and version.
     *
     * @param obj the object to compare
     * @return {@code true} if the keys are equal
     */
    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ServiceKey) && ((ServiceKey)obj).service().equals(service) && (((ServiceKey)obj).version() == version);
    }
  }
}
