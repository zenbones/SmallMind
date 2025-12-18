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

import java.util.Map;
import jakarta.servlet.Servlet;

/**
 * Describes a servlet to be deployed into a Jetty context, including mapping, init parameters, and load order.
 */
public class ServletInstaller extends JettyInstaller {

  private Servlet servlet;
  private Class<? extends Servlet> servletClass;
  private Map<String, String> initParameters;
  private String displayName;
  private String urlPattern;
  private Integer loadOnStartup;
  private Boolean asyncSupported;

  /**
   * Identifies this installer as targeting servlets.
   *
   * @return {@link JettyInstallerType#SERVLET}
   */
  @Override
  public JettyInstallerType getOptionType () {

    return JettyInstallerType.SERVLET;
  }

  /**
   * Retrieves the servlet display name.
   *
   * @return display name for the servlet
   */
  public String getDisplayName () {

    return displayName;
  }

  /**
   * Sets a human-readable display name for the servlet.
   *
   * @param displayName the display name
   */
  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  /**
   * Instantiates or returns the provided servlet instance.
   *
   * @return the servlet to register
   * @throws InstantiationException if the servlet class cannot be instantiated
   * @throws IllegalAccessException if the servlet class or constructor is not accessible
   */
  public Servlet getServlet ()
    throws InstantiationException, IllegalAccessException {

    return (servlet != null) ? servlet : servletClass.newInstance();
  }

  /**
   * Supplies a concrete servlet instance.
   *
   * @param servlet the servlet instance
   */
  public void setServlet (Servlet servlet) {

    this.servlet = servlet;
  }

  /**
   * Supplies the servlet class used when no instance is directly provided.
   *
   * @param servletClass the servlet implementation class
   */
  public void setServletClass (Class<? extends Servlet> servletClass) {

    this.servletClass = servletClass;
  }

  /**
   * Retrieves initialization parameters applied to the servlet.
   *
   * @return map of initialization parameters or {@code null} if none
   */
  public Map<String, String> getInitParameters () {

    return initParameters;
  }

  /**
   * Sets initialization parameters for the servlet.
   *
   * @param initParameters initialization parameters to apply
   */
  public void setInitParameters (Map<String, String> initParameters) {

    this.initParameters = initParameters;
  }

  /**
   * Retrieves the URL pattern to which the servlet will be bound.
   *
   * @return the URL pattern or {@code null} to use {@code /*} by default
   */
  public String getUrlPattern () {

    return urlPattern;
  }

  /**
   * Sets the URL pattern that routes requests to the servlet.
   *
   * @param urlPattern the servlet mapping pattern
   */
  public void setUrlPattern (String urlPattern) {

    this.urlPattern = urlPattern;
  }

  /**
   * Retrieves the configured load-on-startup order value.
   *
   * @return the load-on-startup value or {@code null} to leave unset
   */
  public Integer getLoadOnStartup () {

    return loadOnStartup;
  }

  /**
   * Sets the load-on-startup order for the servlet.
   *
   * @param loadOnStartup order value; lower values load earlier
   */
  public void setLoadOnStartup (Integer loadOnStartup) {

    this.loadOnStartup = loadOnStartup;
  }

  /**
   * Indicates whether the servlet supports asynchronous processing.
   *
   * @return {@code Boolean.TRUE} if async is supported, {@code Boolean.FALSE} if not, or {@code null} if unspecified
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * Configures whether the servlet supports asynchronous processing.
   *
   * @param asyncSupported {@code true} to enable async support, {@code false} to disable, {@code null} to leave unset
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
