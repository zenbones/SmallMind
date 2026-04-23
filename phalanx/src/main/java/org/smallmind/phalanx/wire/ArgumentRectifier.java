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
package org.smallmind.phalanx.wire;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.smallmind.phalanx.wire.signal.Function;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ArgumentInfo;

/**
 * Converts invocation arguments between their Java representations and wire-serializable forms.
 *
 * <p>Two conversion directions are supported:
 * <ul>
 *   <li><b>Inducing</b> — packs a local argument array into a name-keyed map for transmission,
 *       applying {@link WireAdapter} marshalling and enforcing {@link java.io.Serializable}
 *       constraints.</li>
 *   <li><b>Constructing</b> — rebuilds an ordered argument array from an inbound
 *       {@link org.smallmind.phalanx.wire.signal.InvocationSignal}, applying {@link WireAdapter}
 *       unmarshalling and resolving types via the signal codec.</li>
 * </ul>
 * All {@link WireAdapter} instances are cached by class to avoid repeated instantiation.</p>
 */
public class ArgumentRectifier {

  private static final ConcurrentHashMap<Class<? extends WireAdapter<?, ?>>, WireAdapter<?, ?>> ADAPTER_INSTANCE_MAP = new ConcurrentHashMap<>();

  /**
   * Returns a cached {@link WireAdapter} instance for the given class, creating one via its
   * no-argument constructor if none exists yet.
   *
   * @param adapterClass the {@link WireAdapter} implementation class to look up or instantiate
   * @return a cached adapter instance; never {@code null}
   * @throws NoSuchMethodException     if the adapter class lacks a public no-argument constructor
   * @throws IllegalAccessException    if the no-argument constructor is not accessible
   * @throws InvocationTargetException if the constructor throws an exception
   * @throws InstantiationException    if the adapter class is abstract or cannot otherwise be instantiated
   */
  private static WireAdapter<?, ?> getAdapter (Class<? extends WireAdapter<?, ?>> adapterClass)
    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

    WireAdapter<?, ?> adapter;

    if ((adapter = ADAPTER_INSTANCE_MAP.get(adapterClass)) == null) {
      synchronized (ADAPTER_INSTANCE_MAP) {
        if ((adapter = ADAPTER_INSTANCE_MAP.get(adapterClass)) == null) {
          ADAPTER_INSTANCE_MAP.put(adapterClass, adapter = adapterClass.getConstructor().newInstance());
        }
      }
    }

    return adapter;
  }

  /**
   * Marshals a business object to its wire-serializable form using the given adapter.
   *
   * @param adapter  the {@link WireAdapter} to apply
   * @param boundObj the business object to marshal
   * @return the wire-serializable representation of {@code boundObj}
   * @throws Exception if the adapter's marshal operation fails
   */
  private static Object marshal (WireAdapter adapter, Object boundObj)
    throws Exception {

    return adapter.marshal(boundObj);
  }

  /**
   * Unmarshals a wire value back to its business-object form using the given adapter.
   *
   * @param adapter  the {@link WireAdapter} to apply
   * @param valueObj the wire-encoded value to unmarshal
   * @return the reconstructed business object
   * @throws Exception if the adapter's unmarshal operation fails
   */
  private static Object unmarshal (WireAdapter adapter, Object valueObj)
    throws Exception {

    return adapter.unmarshal(valueObj);
  }

  /**
   * Packs a local argument array into a name-keyed map suitable for wire transmission.
   *
   * <p>Each argument is checked for {@link java.io.Serializable} compliance. When an argument's
   * class carries a {@link Wire} annotation, the designated {@link WireAdapter} marshals the
   * value before it is placed in the map; otherwise the value is stored directly. Returns
   * {@code null} when {@code args} is {@code null} or empty.</p>
   *
   * @param argumentNames ordered array of logical argument names drawn from the service definition
   * @param args          ordered array of argument values for the current invocation;
   *                      may be {@code null} or empty
   * @return a map from argument name to its serialized value, or {@code null} if there are
   * no arguments
   * @throws TransportException if an argument is not {@link java.io.Serializable} or adapter
   *                            marshalling fails
   */
  public static HashMap<String, Object> induceMap (String[] argumentNames, Object[] args)
    throws TransportException {

    if ((args == null) || (args.length == 0)) {

      return null;
    } else {

      HashMap<String, Object> argumentMap = new HashMap<>();

      for (int index = 0; index < args.length; index++) {
        if (args[index] == null) {
          argumentMap.put(argumentNames[index], null);
        } else {

          Wire wire;
          Class<?> argumentClass = args[index].getClass();

          if ((wire = argumentClass.getAnnotation(Wire.class)) != null) {
            try {

              WireAdapter<?, ?> adapter = getAdapter(wire.adapter());

              if (!Serializable.class.isAssignableFrom(adapter.getValueType())) {
                throw new TransportException("The argument(index=%d, name=%s, class=%s) is not Serializable", index, argumentNames[index], adapter.getValueType().getName());
              } else {
                argumentMap.put(argumentNames[index], marshal(getAdapter(wire.adapter()), args[index]));
              }
            } catch (Exception exception) {
              throw new TransportException(exception);
            }
          } else if (!Serializable.class.isAssignableFrom(argumentClass)) {
            throw new TransportException("The argument(index=%d, name=%s, class=%s) is not Serializable", index, argumentNames[index], args[index].getClass().getName());
          } else {
            argumentMap.put(argumentNames[index], args[index]);
          }
        }
      }

      return argumentMap;
    }
  }

  /**
   * Rebuilds an ordered argument array from an inbound invocation signal for direct method invocation.
   *
   * <p>Each entry in the signal's argument map is looked up by name in the {@link Methodology},
   * its position within the target method's parameter list is resolved, and its value is
   * deserialized via the {@link SignalCodec}. When the target parameter type carries a
   * {@link Wire} annotation, the appropriate {@link WireAdapter} is applied after codec
   * extraction. Positions corresponding to absent or explicitly {@code null} arguments are
   * left as {@code null}.</p>
   *
   * @param signalCodec        the codec used to deserialize argument values from the signal payload
   * @param invocationSignal   the inbound signal containing the argument map and routing information
   * @param invocationFunction the function descriptor specifying the expected parameter count
   * @param methodology        the {@link Methodology} mapping logical argument names to parameter
   *                           indices and types for the target method
   * @return an argument array aligned to the target method's parameter list; entries may be
   * {@code null} for absent or explicitly null arguments
   * @throws TransportException if an argument name cannot be matched to the method, maps to an
   *                            out-of-range index, or deserialization or unmarshalling fails
   */
  public static Object[] constructArray (SignalCodec signalCodec, InvocationSignal invocationSignal, Function invocationFunction, Methodology methodology)
    throws TransportException {

    Object[] arguments = new Object[invocationFunction.getSignature().length];

    if (invocationSignal.getArguments() != null) {
      for (Map.Entry<String, Object> argumentEntry : invocationSignal.getArguments().entrySet()) {

        ArgumentInfo argumentInfo;

        if ((argumentInfo = methodology.getArgumentInfo(argumentEntry.getKey())) == null) {
          throw new MismatchedArgumentException("Invocation argument(%s) on method(%s) of service(%s) can't be matched by name", argumentEntry.getKey(), invocationFunction.getName(), invocationSignal.getRoute().getService());
        } else if (argumentInfo.getIndex() >= arguments.length) {
          throw new MismatchedArgumentException("Invocation argument(%s) on method(%s) of service(%s) maps to a non-existent argument index(%d)", argumentEntry.getKey(), invocationFunction.getName(), invocationSignal.getRoute().getService(), argumentInfo.getIndex());
        } else if (argumentEntry.getValue() != null) {

          Wire wire;

          if ((wire = argumentInfo.getParameterType().getAnnotation(Wire.class)) != null) {
            try {

              WireAdapter<?, ?> adapter;

              arguments[argumentInfo.getIndex()] = unmarshal(adapter = getAdapter(wire.adapter()), signalCodec.extractObject(argumentEntry.getValue(), adapter.getValueType()));
            } catch (Exception exception) {
              throw new TransportException(exception);
            }
          } else {
            arguments[argumentInfo.getIndex()] = signalCodec.extractObject(argumentEntry.getValue(), argumentInfo.getParameterType());
          }
        }
      }
    }

    return arguments;
  }
}
