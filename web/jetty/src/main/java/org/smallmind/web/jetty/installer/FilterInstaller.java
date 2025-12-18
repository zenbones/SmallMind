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
import jakarta.servlet.Filter;

/**
 * Describes a servlet {@link Filter} to be installed into a Jetty context, including init parameters and URL mapping.
 */
public class FilterInstaller extends JettyInstaller {

  private Filter filter;
  private Class<? extends Filter> filterClass;
  private Map<String, String> initParameters;
  private String displayName;
  private String urlPattern;
  private Boolean asyncSupported;
  private boolean matchAfter = false;

  /**
   * Identifies this installer as targeting filters.
   *
   * @return {@link JettyInstallerType#FILTER}
   */
  @Override
  public JettyInstallerType getOptionType () {

    return JettyInstallerType.FILTER;
  }

  /**
   * Retrieves the display name that will appear in Jetty for the filter.
   *
   * @return filter display name
   */
  public String getDisplayName () {

    return displayName;
  }

  /**
   * Sets the display name for the filter.
   *
   * @param displayName human-readable name shown in Jetty
   */
  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  /**
   * Instantiates or returns the provided filter instance.
   *
   * @return the filter to register
   * @throws InstantiationException if the filter class cannot be instantiated
   * @throws IllegalAccessException if the filter class or its nullary constructor is not accessible
   */
  public Filter getFilter ()
    throws InstantiationException, IllegalAccessException {

    return (filter != null) ? filter : filterClass.newInstance();
  }

  /**
   * Supplies a concrete filter instance to register.
   *
   * @param filter the filter instance
   */
  public void setFilter (Filter filter) {

    this.filter = filter;
  }

  /**
   * Supplies the filter class to instantiate if an instance is not provided directly.
   *
   * @param filterClass the filter implementation class
   */
  public void setFilterClass (Class<? extends Filter> filterClass) {

    this.filterClass = filterClass;
  }

  /**
   * Retrieves initialization parameters that will be applied to the filter.
   *
   * @return map of initialization parameters or {@code null} if none
   */
  public Map<String, String> getInitParameters () {

    return initParameters;
  }

  /**
   * Sets initialization parameters for the filter.
   *
   * @param initParameters parameters to be passed to the filter
   */
  public void setInitParameters (Map<String, String> initParameters) {

    this.initParameters = initParameters;
  }

  /**
   * Indicates whether this filter should be inserted after existing mappings.
   *
   * @return {@code true} if the filter should be matched after current filters
   */
  public boolean isMatchAfter () {

    return matchAfter;
  }

  /**
   * Configures whether this filter is matched after other filters for the same pattern.
   *
   * @param matchAfter {@code true} to append after existing filters
   */
  public void setMatchAfter (boolean matchAfter) {

    this.matchAfter = matchAfter;
  }

  /**
   * Retrieves the URL pattern that determines where the filter applies.
   *
   * @return the URL pattern or {@code null} to default to {@code /*}
   */
  public String getUrlPattern () {

    return urlPattern;
  }

  /**
   * Sets the URL pattern to which the filter will be mapped.
   *
   * @param urlPattern the URL pattern or {@code null} to use the default
   */
  public void setUrlPattern (String urlPattern) {

    this.urlPattern = urlPattern;
  }

  /**
   * Indicates whether the filter supports asynchronous dispatch.
   *
   * @return {@code Boolean.TRUE} if async is supported, {@code Boolean.FALSE} if not, or {@code null} to leave default
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * Configures whether the filter supports asynchronous dispatch.
   *
   * @param asyncSupported {@code true} to enable async support, {@code false} to disable, {@code null} to leave unset
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
