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
package org.smallmind.javafx.extras.instrument;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javafx.application.Platform;
import org.smallmind.javafx.extras.dialog.JavaErrorDialog;

/**
 * {@link VelocityChart} that polls a JMX MBean for velocity metrics and plots the values.
 */
public class JMXVelocityChart extends VelocityChart {

  private static final String[] DISTRIBUTION_ATTRIBUTES = new String[] {"AverageVelocity", "OneMinuteAvgVelocity", "OneMinuteAvgVelocity", "OneMinuteAvgVelocity"};
  private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(2, new ThreadFactory() {

    @Override
    public Thread newThread (final Runnable runnable) {

      Thread thread = new Thread(runnable);

      thread.setDaemon(true);

      return thread;
    }
  });
  private final MBeanServerConnection mBeanServerConnection;
  private final ObjectName objectName;
  private final ScheduledFuture<?> future;

  /**
   * Constructs the chart and begins polling the specified MBean for velocity attributes.
   *
   * @param spanInMilliseconds    the time span to display
   * @param mBeanServerConnection the MBean server connection used for polling
   * @param objectName            the object name of the MBean exposing the velocity attributes
   */
  public JMXVelocityChart (long spanInMilliseconds, MBeanServerConnection mBeanServerConnection, ObjectName objectName) {

    super(spanInMilliseconds);

    this.mBeanServerConnection = mBeanServerConnection;
    this.objectName = objectName;

    setTitle("Attribute/get");
    getXAxis().setLabel("Time");
    getYAxis().setLabel("Velocity (qty/sec)");

    future = SCHEDULED_EXECUTOR.scheduleAtFixedRate(new Runnable() {

      @Override
      public void run () {

        collectData();
      }
    }, 1, 15, TimeUnit.SECONDS);
  }

  /**
   * Retrieves the latest velocity measurements from the MBean and adds them to the chart. If an error occurs, polling
   * is paused and a {@link JavaErrorDialog} is shown on the JavaFX thread.
   */
  private void collectData () {

    if (!isPaused()) {
      try {

        AttributeList attributeList = mBeanServerConnection.getAttributes(objectName, DISTRIBUTION_ATTRIBUTES);

        addBlur(System.currentTimeMillis(), new Blur((Double)((Attribute)attributeList.get(0)).getValue(), (Double)((Attribute)attributeList.get(1)).getValue(), (Double)((Attribute)attributeList.get(2)).getValue(), (Double)((Attribute)attributeList.get(3)).getValue()));
      } catch (final Exception exception) {
        setPaused(true);

        Platform.runLater(new Runnable() {

          @Override
          public void run () {

            JavaErrorDialog.showJavaErrorDialog(this, exception);
          }
        });
      }
    }
  }

  /**
   * Cancels polling and stops the chart's time axis.
   */
  @Override
  public void stop () {

    future.cancel(false);
    super.stop();
  }
}
