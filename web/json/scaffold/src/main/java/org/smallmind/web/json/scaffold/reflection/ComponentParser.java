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
package org.smallmind.web.json.scaffold.reflection;

import java.io.IOException;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;

/**
 * Parses dotted bean/method paths containing optional parameter lists and array subscripts
 * into {@link PathComponent} elements for reflective execution.
 */
public class ComponentParser {

  private enum State {START_COMPONENT, START_METHOD_NAME, METHOD_NAME, END_METHOD_NAME, START_PARAMETERS, PARAMETERS, STRING, ESCAPE, END_PARAMETERS, START_SUBSCRIPT, END_SUBSCRIPT}

  /**
   * Parses a method/property chain into an array of {@link PathComponent} instances.
   *
   * @param methodChain expression such as {@code user.getAddress()[0].zipCode}
   * @return ordered array of path components
   * @throws BeanAccessException if the chain is syntactically invalid
   */
  public static PathComponent[] parse (String methodChain)
    throws BeanAccessException {

    PathComponent[] components;
    LinkedList<PathComponent> componentList = new LinkedList<>();

    if (methodChain != null) {

      PathComponent component = null;
      State state = State.START_COMPONENT;
      int mark = -1;
      int index = 0;

      while (index < methodChain.length()) {
        switch (state) {
          case START_COMPONENT:
            if (component != null) {
              componentList.add(component);
              component = null;
            }
            if (!Character.isWhitespace(methodChain.charAt(index))) {
              state = State.START_METHOD_NAME;
            } else {
              index++;
            }
            break;
          case START_METHOD_NAME:
            if (!Character.isJavaIdentifierStart(methodChain.charAt(index))) {
              throw new BeanAccessException("Illegal java identifier at index(%d) in method chain(%s)", index, methodChain);
            }
            mark = index++;
            state = State.METHOD_NAME;
            break;
          case METHOD_NAME:
            switch (methodChain.charAt(index)) {
              case '.':
                state = State.START_COMPONENT;
                component = new PathComponent(methodChain.substring(mark, index));
                break;
              case '(':
                state = State.START_PARAMETERS;
                component = new PathComponent(methodChain.substring(mark, index));
                break;
              case '[':
                state = State.START_SUBSCRIPT;
                component = new PathComponent(methodChain.substring(mark, index));
                mark = index + 1;
                break;
              default:
                if (Character.isWhitespace(methodChain.charAt(index))) {
                  state = State.END_METHOD_NAME;
                  component = new PathComponent(methodChain.substring(mark, index));
                } else if (!Character.isJavaIdentifierPart(methodChain.charAt(index))) {
                  throw new BeanAccessException("Illegal java identifier at index(%d) in method chain(%s)", index, methodChain);
                }
            }
            index++;
            break;
          case END_METHOD_NAME:
            switch (methodChain.charAt(index)) {
              case '.':
                state = State.START_COMPONENT;
                break;
              case '(':
                state = State.START_PARAMETERS;
                break;
              case '[':
                mark = index + 1;
                state = State.START_SUBSCRIPT;
                break;
              default:
                if (!Character.isWhitespace(methodChain.charAt(index))) {
                  throw new BeanAccessException("Illegal java identifier at index(%d) in method chain(%s)", index, methodChain);
                }
            }
            index++;
            break;
          case START_PARAMETERS:
            mark = index;
            state = State.PARAMETERS;
            break;
          case PARAMETERS:
            switch (methodChain.charAt(index)) {
              case '"':
                state = State.STRING;
                break;
              case ')':
                try {
                  component.createArguments(methodChain.substring(mark, index));
                } catch (IOException ioException) {
                  throw new BeanAccessException("Illegal parameter arguments at index(%d) in method chain(%s)", index, methodChain);
                }
                state = State.END_PARAMETERS;
                break;
            }
            index++;
            break;
          case STRING:
            switch (methodChain.charAt(index)) {
              case '\\':
                state = State.ESCAPE;
                break;
              case '"':
                state = State.PARAMETERS;
                break;
            }
            index++;
            break;
          case ESCAPE:
            state = State.STRING;
            index++;
            break;
          case END_PARAMETERS:
            switch (methodChain.charAt(index)) {
              case '.':
                state = State.START_COMPONENT;
                break;
              case '[':
                mark = index + 1;
                state = State.START_SUBSCRIPT;
                break;
              default:
                if (!Character.isWhitespace(methodChain.charAt(index))) {
                  throw new BeanAccessException("Illegal start of component at index(%d) in method chain(%s)", index, methodChain);
                }
            }
            index++;
            break;
          case START_SUBSCRIPT:
            if (methodChain.charAt(index) == ']') {
              try {
                component.addSubscript(methodChain.substring(mark, index).strip());
              } catch (NumberFormatException numberFormatException) {
                throw new BeanAccessException("Illegal subscript at index(%d) in method chain(%s)", mark, methodChain);
              }
              state = State.END_SUBSCRIPT;
            }
            index++;
            break;
          case END_SUBSCRIPT:
            if (methodChain.charAt(index) == '.') {
              state = State.START_COMPONENT;
            } else if (!Character.isWhitespace(methodChain.charAt(index))) {
              throw new BeanAccessException("Illegal start of component at index(%d) in method chain(%s)", index, methodChain);
            }
            index++;
            break;
        }
      }

      switch (state) {
        case START_COMPONENT:
          if (componentList.isEmpty()) {
            throw new BeanAccessException("Empty method chain", methodChain);
          }

          throw new BeanAccessException("Missing termination in method chain(%s)", methodChain);
        case METHOD_NAME:
          componentList.add(new PathComponent(methodChain.substring(mark, index)));
          break;
        case END_METHOD_NAME:
          componentList.add(component);
          break;
        case END_PARAMETERS:
          componentList.add(component);
          break;
        case END_SUBSCRIPT:
          componentList.add(component);
          break;
        default:
          throw new BeanAccessException("Missing termination in method chain(%s)", methodChain);
      }
    }

    components = new PathComponent[componentList.size()];
    componentList.toArray(components);

    return components;
  }
}
