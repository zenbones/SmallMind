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
package org.smallmind.web.json.scaffold.reflection;

import java.io.IOException;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;

public class ComponentParser {

  private enum State {START_COMPONENT, START_METHOD_NAME, METHOD_NAME, END_METHOD_NAME, START_PARAMETERS, PARAMETERS, STRING, ESCAPE, END_PARAMETERS, START_SUBSCRIPT, SUBSCRIPT}

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
                state = State.START_SUBSCRIPT;
                break;
              default:
                if (!Character.isWhitespace(methodChain.charAt(index))) {
                  throw new BeanAccessException("Illegal java identifier at index(%d) in method chain(%s)", index, methodChain);
                }
            }
            index++;
            break;
          case START_SUBSCRIPT:
            if (Character.isDigit(methodChain.charAt(index))) {
              mark = index;
              state = State.SUBSCRIPT;
            } else if (!Character.isWhitespace(methodChain.charAt(index))) {
              throw new BeanAccessException("Illegal subscript at index(%d) in method chain(%s)", index, methodChain);
            }
            index++;
            break;
          case SUBSCRIPT:
            if (methodChain.charAt(index) == ']') {
              try {
                component.addSubscript(methodChain.substring(mark, index));
              } catch (NumberFormatException numberFormatException) {
                throw new BeanAccessException("Illegal subscript at index(%d) in method chain(%s)", mark, methodChain);
              }
              state = State.END_PARAMETERS;
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
        default:
          throw new BeanAccessException("Missing termination in method chain(%s)", methodChain);
      }
    }

    components = new PathComponent[componentList.size()];
    componentList.toArray(components);

    return components;
  }
}
