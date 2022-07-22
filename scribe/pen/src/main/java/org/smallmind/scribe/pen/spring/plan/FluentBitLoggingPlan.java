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
package org.smallmind.scribe.pen.spring.plan;

import java.text.SimpleDateFormat;
import java.util.Map;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.ConsoleAppender;
import org.smallmind.scribe.pen.DateFormatTimestamp;
import org.smallmind.scribe.pen.DefaultErrorHandler;
import org.smallmind.scribe.pen.FluentBitAppender;
import org.smallmind.scribe.pen.PatternFormatter;

/*
          <bean class="org.smallmind.scribe.pen.FluentAppender">
            <constructor-arg index="0" name="name" value="${epicenter.artifact}"/>
            <property name="host" value="${log.fluent.host}"/>
            <property name="port" value="${log.fluent.port}"/>
            <property name="additionalEventData">
              <map>
                <entry key="stream" value="service_log"/>
                <entry key="podName" value="${k8s.pod_name}"/>
                <entry key="podNamespace" value="${k8s.pod_namespace}"/>
                <entry key="nodeName" value="${k8s.node_name}"/>
                <entry key="helmReleaseName" value="${k8s.helm_release_name}"/>
                <entry key="epicenterEnvironment" value="${epicenter.environment}"/>
                <entry key="epicenterArtifact" value="${epicenter.artifact}"/>
              </map>
            </property>
            <property name="errorHandler" ref="consoleErrorHandler"/>
          </bean>
 */

public class FluentBitLoggingPlan extends LoggingPlan {

  private DateFormatTimestamp fullTimestamp = new DateFormatTimestamp(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
  private Map<String, String> additionalEventData;
  private String name;
  private String host;
  private int port;

  public void setFullTimestamp (DateFormatTimestamp fullTimestamp) {

    this.fullTimestamp = fullTimestamp;
  }

  public void setAdditionalEventData (Map<String, String> additionalEventData) {

    this.additionalEventData = additionalEventData;
  }

  public void setName (String name) {

    this.name = name;
  }

  public void setHost (String host) {

    this.host = host;
  }

  public void setPort (int port) {

    this.port = port;
  }

  @Override
  public Appender getAppender () {

    FluentBitAppender fluentBitAppender = new FluentBitAppender(name, new DefaultErrorHandler(new ConsoleAppender(new PatternFormatter(fullTimestamp, "%d %n %+5l (%.1C.%M:%L) [%T] - %m%!+\n\t!p%!+\n\t!s"))));

    fluentBitAppender.setHost(host);
    fluentBitAppender.setPort(port);
    fluentBitAppender.setAdditionalEventData(additionalEventData);

    fluentBitAppender.afterPropertiesSet();

    return fluentBitAppender;
  }
}
