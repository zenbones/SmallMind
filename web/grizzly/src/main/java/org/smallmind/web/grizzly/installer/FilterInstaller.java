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

import java.util.Map;
import jakarta.servlet.Filter;

/**
 * Describes a servlet filter to be installed into a Grizzly web application context.
 */
public class FilterInstaller extends GrizzlyInstaller {

  private Filter filter;
  private Class<? extends Filter> filterClass;
  private Map<String, String> initParameters;
  private String displayName;
  private String urlPattern;
  private Boolean asyncSupported;
  private boolean matchAfter = false;

  /**
   * @return {@link GrizzlyInstallerType#FILTER}
   */
  @Override
  public GrizzlyInstallerType getOptionType () {

    return GrizzlyInstallerType.FILTER;
  }

  /**
   * @return filter display name used when registering with the servlet context
   */
  public String getDisplayName () {

    return displayName;
  }

  /**
   * @param displayName friendly name for the filter registration
   */
  public void setDisplayName (String displayName) {

    this.displayName = displayName;
  }

  /**
   * Instantiates the filter instance if a class was supplied, or returns the provided instance.
   *
   * @return filter instance to deploy
   * @throws InstantiationException if the filter cannot be instantiated
   * @throws IllegalAccessException if the filter class or constructor is not accessible
   */
  public Filter getFilter ()
    throws InstantiationException, IllegalAccessException {

    return (filter != null) ? filter : filterClass.newInstance();
  }

  /**
   * @param filter concrete filter instance to register
   */
  public void setFilter (Filter filter) {

    this.filter = filter;
  }

  /**
   * @param filterClass filter implementation class to instantiate when deploying
   */
  public void setFilterClass (Class<? extends Filter> filterClass) {

    this.filterClass = filterClass;
  }

  /**
   * @return init parameters to be supplied during filter registration
   */
  public Map<String, String> getInitParameters () {

    return initParameters;
  }

  /**
   * @param initParameters init parameters supplied to the servlet context
   */
  public void setInitParameters (Map<String, String> initParameters) {

    this.initParameters = initParameters;
  }

  /**
   * @return {@code true} if the filter should be matched after existing mappings
   */
  public boolean isMatchAfter () {

    return matchAfter;
  }

  /**
   * @param matchAfter whether the mapping should occur after existing filters
   */
  public void setMatchAfter (boolean matchAfter) {

    this.matchAfter = matchAfter;
  }

  /**
   * @return URL pattern to map the filter to; defaults to {@code /*} when {@code null}
   */
  public String getUrlPattern () {

    return urlPattern;
  }

  /**
   * @param urlPattern URL pattern for the filter mapping
   */
  public void setUrlPattern (String urlPattern) {

    this.urlPattern = urlPattern;
  }

  /**
   * @return whether the filter supports async processing; {@code null} uses container default
   */
  public Boolean getAsyncSupported () {

    return asyncSupported;
  }

  /**
   * @param asyncSupported flag indicating async support
   */
  public void setAsyncSupported (Boolean asyncSupported) {

    this.asyncSupported = asyncSupported;
  }
}
