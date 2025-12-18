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
package org.smallmind.web.jetty.installer;

import java.lang.reflect.InvocationTargetException;
import java.util.EventListener;
import java.util.Map;

/**
 * Describes a servlet {@link EventListener} to register with a Jetty context, along with optional context parameters.
 */
public class ListenerInstaller extends JettyInstaller {

  private EventListener eventListener;
  private Class<? extends EventListener> listenerClass;
  private Map<String, String> contextParameters;

  /**
   * Identifies this installer as targeting servlet listeners.
   *
   * @return {@link JettyInstallerType#LISTENER}
   */
  @Override
  public JettyInstallerType getOptionType () {

    return JettyInstallerType.LISTENER;
  }

  /**
   * Instantiates or returns the provided servlet listener.
   *
   * @return the listener instance to register
   * @throws NoSuchMethodException     if the listener class lacks a no-argument constructor
   * @throws InstantiationException    if the listener cannot be instantiated
   * @throws IllegalAccessException    if the constructor is inaccessible
   * @throws InvocationTargetException if the constructor throws an exception
   */
  public EventListener getListener ()
    throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    return (eventListener != null) ? eventListener : listenerClass.getDeclaredConstructor().newInstance();
  }

  /**
   * Supplies a concrete listener instance.
   *
   * @param eventListener the listener to register
   */
  public void setEventListener (EventListener eventListener) {

    this.eventListener = eventListener;
  }

  /**
   * Supplies the listener class used when an instance is not directly provided.
   *
   * @param listenerClass the listener implementation class
   */
  public void setListenerClass (Class<? extends EventListener> listenerClass) {

    this.listenerClass = listenerClass;
  }

  /**
   * Retrieves the context initialization parameters applied alongside the listener.
   *
   * @return map of context parameters or {@code null} if none
   */
  public Map<String, String> getContextParameters () {

    return contextParameters;
  }

  /**
   * Sets context initialization parameters to accompany this listener.
   *
   * @param contextParameters parameters to apply to the servlet context
   */
  public void setContextParameters (Map<String, String> contextParameters) {

    this.contextParameters = contextParameters;
  }
}
