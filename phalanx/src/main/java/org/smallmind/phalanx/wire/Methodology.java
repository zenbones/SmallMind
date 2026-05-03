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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.smallmind.phalanx.wire.transport.ArgumentInfo;
import org.smallmind.phalanx.wire.transport.SyntheticArgument;

/**
 * Holds reflective metadata for a single service method, mapping each logical argument name
 * to its parameter index and declared type.
 *
 * <p>Argument names are resolved from one of two sources, tried in order:
 * <ol>
 *   <li><b>Synthetic arguments</b> — an explicit {@link SyntheticArgument} array supplied at
 *       construction time, used when annotation-based discovery is impractical (e.g., for
 *       generated proxies).</li>
 *   <li><b>{@link Argument} annotations</b> — per-parameter {@code @Argument} annotations
 *       present on the method in the service interface.</li>
 * </ol>
 * Construction throws {@link ServiceDefinitionException} when the number of resolved argument
 * names does not match the method's actual parameter count.
 */
public class Methodology {

  private final Method method;
  private final HashMap<String, ArgumentInfo> argumentInfoMap = new HashMap<>();

  /**
   * Constructs a {@code Methodology} for the given service method.
   *
   * @param serviceInterface   the interface that declares {@code method}; used only in
   *                           diagnostic messages
   * @param method             the service method whose argument metadata is to be captured
   * @param syntheticArguments optional explicit argument descriptors; when non-empty, these
   *                           take precedence over any {@link Argument} annotations on the method
   * @throws ServiceDefinitionException if the number of resolved argument names does not equal
   *                                    the method's parameter count, indicating missing
   *                                    {@link Argument} annotations
   */
  public Methodology (Class<?> serviceInterface, Method method, SyntheticArgument... syntheticArguments)
    throws ServiceDefinitionException {

    int index = 0;

    this.method = method;

    if ((syntheticArguments != null) && (syntheticArguments.length > 0)) {
      for (SyntheticArgument syntheticArgument : syntheticArguments) {
        argumentInfoMap.put(syntheticArgument.getName(), new ArgumentInfo(index++, syntheticArgument.getParameterType()));
      }
    } else {
      for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
        for (Annotation annotation : parameterAnnotations) {
          if (annotation.annotationType().equals(Argument.class)) {
            argumentInfoMap.put(((Argument)annotation).value(), new ArgumentInfo(index, method.getParameterTypes()[index++]));
            break;
          }
        }
      }
    }

    if (index != method.getParameterTypes().length) {
      throw new ServiceDefinitionException("The method(%s) of service interface(%s) requires @Argument annotations", method.getName(), serviceInterface.getName());
    }
  }

  /**
   * Returns the service method this {@code Methodology} describes.
   *
   * @return the reflected {@link Method}; never {@code null}
   */
  public Method getMethod () {

    return method;
  }

  /**
   * Returns the argument metadata registered under the given logical name.
   *
   * @param name the logical argument name as declared via {@link Argument} or a
   *             {@link SyntheticArgument}
   * @return the {@link ArgumentInfo} containing the parameter's index and declared type,
   * or {@code null} if no argument with that name was registered
   */
  public ArgumentInfo getArgumentInfo (String name) {

    return argumentInfoMap.get(name);
  }
}
