/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.smallmind.phalanx.wire.signal.Function;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ArgumentInfo;

public class ArgumentRectifier {

  public static HashMap<String, Object> induceMap (Method method, String[] argumentNames, Object[] args)
    throws TransportException {

    if ((args == null) || (args.length == 0)) {

      return null;
    } else {

      HashMap<String, Object> argumentMap = new HashMap<>();

      for (int index = 0; index < args.length; index++) {
        if ((args[index] != null) && (!(args[index] instanceof Serializable))) {
          throw new TransportException("The argument(index=%d, name=%s, class=%s) is not Serializable", index, argumentNames[index], args[index].getClass().getName());
        }

        argumentMap.put(argumentNames[index], args[index]);
      }

      return argumentMap;
    }
  }

  public static Object[] constructArray (SignalCodec signalCodec, InvocationSignal invocationSignal, Function invocationFunction, Methodology methodology)
    throws MismatchedArgumentException {

    Object[] arguments = new Object[invocationFunction.getSignature().length];

    if (invocationSignal.getArguments() != null) {
      for (Map.Entry<String, Object> argumentEntry : invocationSignal.getArguments().entrySet()) {

        ArgumentInfo argumentInfo;

        if ((argumentInfo = methodology.getArgumentInfo(argumentEntry.getKey())) == null) {
          throw new MismatchedArgumentException("Invocation argument(%s) on method(%s) of service(%s) can't be matched by name", argumentEntry.getKey(), invocationFunction.getName(), invocationSignal.getRoute().getService());
        }
        if (argumentInfo.getIndex() >= arguments.length) {
          throw new MismatchedArgumentException("Invocation argument(%s) on method(%s) of service(%s) maps to a non-existent argument index(%d)", argumentEntry.getKey(), invocationFunction.getName(), invocationSignal.getRoute().getService(), argumentInfo.getIndex());
        }

        arguments[argumentInfo.getIndex()] = signalCodec.extractObject(argumentEntry.getValue(), argumentInfo.getParameterType());
      }
    }

    return arguments;
  }
}

