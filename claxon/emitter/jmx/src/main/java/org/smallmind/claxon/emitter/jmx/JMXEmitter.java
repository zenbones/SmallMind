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
package org.smallmind.claxon.emitter.jmx;

import java.util.Hashtable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.smallmind.claxon.registry.PushEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;

/**
 * Push emitter that exposes Claxon metrics as JMX dynamic MBeans and keeps their attributes
 * up to date on every recording cycle.
 *
 * <p>On the first invocation of {@link #record(String, Tag[], Quantity[])} for a given
 * meter+tag combination, a {@link MeterDynamicMbean} is registered in the supplied
 * {@link MBeanServer} under an {@link ObjectName} whose domain is the meter name and whose
 * key-properties are derived from the provided tags. Subsequent invocations update the MBean
 * attributes in place without re-registering the MBean.
 *
 * <p>Registration is guarded by a double-checked locking pattern on the {@link MBeanServer}
 * monitor to avoid duplicate registration under concurrent access.
 */
public class JMXEmitter extends PushEmitter {

  /**
   * The JMX {@link MBeanServer} used to register and update meter MBeans.
   */
  private final MBeanServer server;

  /**
   * Creates a JMX emitter that registers and updates MBeans in the specified server.
   *
   * @param server the {@link MBeanServer} in which meter MBeans will be registered; must not
   *               be {@code null}
   */
  public JMXEmitter (MBeanServer server) {

    this.server = server;
  }

  /**
   * Registers a {@link MeterDynamicMbean} for the meter if one does not already exist, then
   * updates its attributes with the current quantity values.
   *
   * <p>Tags are translated into JMX {@link ObjectName} key-properties so that each unique
   * meter+tag combination maps to a distinct MBean. Quantities are mapped to MBean attributes
   * whose names match {@link Quantity#getName()}.
   *
   * @param meterName  the meter name used as the domain portion of the JMX {@link ObjectName}
   * @param tags       tags translated to key-property pairs in the {@link ObjectName}; may be
   *                   {@code null} or empty
   * @param quantities the current measured values to store as MBean attributes; must not be
   *                   {@code null}
   * @throws MalformedObjectNameException   if the constructed {@link ObjectName} is invalid
   * @throws NotCompliantMBeanException     if the {@link MeterDynamicMbean} does not comply
   *                                        with JMX MBean conventions
   * @throws MBeanRegistrationException     if the MBean server rejects the registration
   * @throws InstanceAlreadyExistsException if a race condition causes a duplicate registration
   *                                        attempt to reach the MBean server
   * @throws InstanceNotFoundException      if the MBean cannot be found when updating
   *                                        attributes
   * @throws ReflectionException            if an error occurs while setting attributes
   *                                        reflectively
   */
  @Override
  public void record (String meterName, Tag[] tags, Quantity[] quantities)
    throws MalformedObjectNameException, NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, InstanceNotFoundException, ReflectionException {

    ObjectName objectName;
    AttributeList attributeList = new AttributeList();
    Hashtable<String, String> tagTable = new Hashtable<>();

    if ((tags != null) && (tags.length > 0)) {
      for (Tag tag : tags) {
        tagTable.put(tag.getKey(), tag.getValue());
      }
    }

    objectName = new ObjectName(meterName, tagTable);

    if (!server.isRegistered(objectName)) {
      synchronized (server) {
        if (!server.isRegistered(objectName)) {
          server.registerMBean(new MeterDynamicMbean(meterName, tags, quantities), objectName);
        }
      }
    }

    for (Quantity quantity : quantities) {
      attributeList.add(new Attribute(quantity.getName(), quantity.getValue()));
    }

    server.setAttributes(objectName, attributeList);
  }
}
