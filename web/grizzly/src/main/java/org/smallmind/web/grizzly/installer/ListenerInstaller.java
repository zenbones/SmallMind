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
package org.smallmind.web.grizzly.installer;

import java.lang.reflect.InvocationTargetException;
import java.util.EventListener;
import java.util.Map;

/**
 * Describes a servlet context listener to register within a Grizzly application context.
 */
public class ListenerInstaller extends GrizzlyInstaller {

  private EventListener eventListener;
  private Class<? extends EventListener> listenerClass;
  private Map<String, String> contextParameters;

  /**
   * @return {@link GrizzlyInstallerType#LISTENER}
   */
  @Override
  public GrizzlyInstallerType getOptionType () {

    return GrizzlyInstallerType.LISTENER;
  }

  /**
   * Instantiates or returns the configured listener.
   *
   * @return listener to install
   * @throws NoSuchMethodException     if the default constructor cannot be found
   * @throws InstantiationException    if the listener cannot be constructed
   * @throws IllegalAccessException    if the constructor is not accessible
   * @throws InvocationTargetException if the constructor throws an exception
   */
  public EventListener getListener ()
    throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    return (eventListener != null) ? eventListener : listenerClass.getDeclaredConstructor().newInstance();
  }

  /**
   * @param eventListener concrete listener instance
   */
  public void setEventListener (EventListener eventListener) {

    this.eventListener = eventListener;
  }

  /**
   * @param listenerClass listener implementation class to instantiate
   */
  public void setListenerClass (Class<? extends EventListener> listenerClass) {

    this.listenerClass = listenerClass;
  }

  /**
   * @return context initialization parameters to add when registering the listener
   */
  public Map<String, String> getContextParameters () {

    return contextParameters;
  }

  /**
   * @param contextParameters additional context parameters for the web application
   */
  public void setContextParameters (Map<String, String> contextParameters) {

    this.contextParameters = contextParameters;
  }
}
