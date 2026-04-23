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

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;

/**
 * JMX {@link DynamicMBean} that represents a single Claxon meter, exposing its quantities as
 * readable {@code double}-valued attributes.
 *
 * <p>The set of attribute names is fixed at construction time and is derived from the
 * {@link Quantity} array supplied to the constructor. Attributes are stored in a
 * {@link ConcurrentHashMap} and may be updated concurrently by the JMX emitter. Attribute
 * values default to {@code 0} when they have not yet been written. Operation invocation is
 * not supported and always throws {@link UnsupportedOperationException}.
 */
public class MeterDynamicMbean implements DynamicMBean {

  /**
   * Immutable JMX metadata describing this MBean's class name, description, and attribute
   * definitions; constructed once during initialisation.
   */
  private final MBeanInfo mBeanInfo;

  /**
   * Set of valid attribute names derived from the quantities supplied at construction time;
   * used to validate get and set requests.
   */
  private final HashSet<String> attributeNameSet = new HashSet<>();

  /**
   * Concurrent map from attribute name to its current {@code double} value; updated by
   * {@link #setAttribute(Attribute)} and {@link #setAttributes(AttributeList)}.
   */
  private final ConcurrentHashMap<String, Double> valueMap = new ConcurrentHashMap<>();

  /**
   * Constructs a dynamic MBean whose attributes correspond to the provided quantities.
   *
   * <p>The MBean description is built from the meter name and tags for human-readable
   * identification in JMX consoles. Each quantity becomes a read-only {@code double}-typed
   * attribute.
   *
   * @param name       the meter name included in the MBean description
   * @param tags       the tags included in the MBean description; may be {@code null} or empty
   * @param quantities the quantities that define the exposed attributes; may be {@code null}
   *                   or empty
   */
  public MeterDynamicMbean (String name, Tag[] tags, Quantity[] quantities) {

    MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[(quantities == null) ? 0 : quantities.length];
    StringBuilder descriptionBuilder = new StringBuilder("Meter MBean(name=").append(name).append(", tags[");

    if ((tags != null) && (tags.length > 0)) {

      boolean first = true;

      for (Tag tag : tags) {
        if (!first) {
          descriptionBuilder.append(", ");
        }
        descriptionBuilder.append(tag.getKey()).append("=").append(tag.getValue());
        first = false;
      }
    }
    descriptionBuilder.append("])");

    if ((quantities != null) && (quantities.length > 0)) {

      int index = 0;

      for (Quantity quantity : quantities) {
        attributeNameSet.add(quantity.getName());
        attributeInfos[index++] = new MBeanAttributeInfo(quantity.getName(), Double.class.getName(), quantity.getName(), true, false, false);
      }
    }

    mBeanInfo = new MBeanInfo(MeterDynamicMbean.class.getName(), descriptionBuilder.toString(), attributeInfos, null, null, null);
  }

  /**
   * Returns the current value of the named attribute.
   *
   * <p>If the attribute has not been set yet, {@code 0} is returned as the default value.
   *
   * @param attribute the name of the attribute to retrieve
   * @return the current attribute value as a {@link Double}, or {@code 0} when unset
   * @throws AttributeNotFoundException if {@code attribute} is not among the attributes
   *                                    defined at construction time
   */
  @Override
  public Object getAttribute (String attribute)
    throws AttributeNotFoundException {

    if (attributeNameSet.contains(attribute)) {

      Double value;

      return ((value = valueMap.get(attribute)) == null) ? 0 : value;
    } else {
      throw new AttributeNotFoundException(attribute);
    }
  }

  /**
   * Sets the value of a single named attribute.
   *
   * <p>The attribute value may be supplied as any {@link Number} subtype (in which case
   * {@link Number#doubleValue()} is used) or as a {@link String} parseable by
   * {@link Double#parseDouble(String)}.
   *
   * @param attribute the attribute to set, identified by name and carrying a new value
   * @throws AttributeNotFoundException     if the attribute name is not defined for this MBean
   * @throws InvalidAttributeValueException if the attribute value cannot be converted to a
   *                                        {@code double}
   */
  @Override
  public void setAttribute (Attribute attribute)
    throws AttributeNotFoundException, InvalidAttributeValueException {

    if (attributeNameSet.contains(attribute.getName())) {
      if (Number.class.isAssignableFrom(attribute.getValue().getClass())) {
        valueMap.put(attribute.getName(), ((Number)attribute.getValue()).doubleValue());
      } else if (String.class.equals(attribute.getValue().getClass())) {
        valueMap.put(attribute.getName(), Double.parseDouble((String)attribute.getValue()));
      } else {
        throw new InvalidAttributeValueException("Requires a double value");
      }
    } else {
      throw new AttributeNotFoundException(attribute.getName());
    }
  }

  /**
   * Returns the values of the requested attributes that are currently stored.
   *
   * <p>Attributes that have not yet been written are omitted from the result rather than
   * defaulting to zero, matching standard JMX conventions for bulk retrieval.
   *
   * @param attributes array of attribute names whose values should be returned
   * @return an {@link AttributeList} containing an {@link Attribute} entry for each requested
   * name that has a stored value
   */
  @Override
  public AttributeList getAttributes (String[] attributes) {

    AttributeList attributeList = new AttributeList();

    for (String attribute : attributes) {

      Double value;

      if ((value = valueMap.get(attribute)) != null) {
        attributeList.add(new Attribute(attribute, value));
      }
    }

    return attributeList;
  }

  /**
   * Sets multiple attribute values in a single call, silently skipping entries that are not
   * {@link Attribute} instances, have unknown names, or carry values that cannot be stored as
   * {@code double}.
   *
   * <p>Only attributes whose values are primitive {@code double}, {@link Double}, or
   * {@link String} are accepted; all others are ignored without throwing.
   *
   * @param attributes the attributes to set
   * @return an {@link AttributeList} containing only the attributes that were successfully
   * stored
   */
  @Override
  public AttributeList setAttributes (AttributeList attributes) {

    AttributeList setAttributeList = new AttributeList();

    for (Object obj : attributes) {
      if ((obj instanceof Attribute) && attributeNameSet.contains(((Attribute)obj).getName())) {
        if (double.class.equals(((Attribute)obj).getValue().getClass())) {
          setAttributeList.add(obj);
          valueMap.put(((Attribute)obj).getName(), (double)((Attribute)obj).getValue());
        } else if (Double.class.equals(((Attribute)obj).getValue().getClass())) {
          setAttributeList.add(obj);
          valueMap.put(((Attribute)obj).getName(), (Double)((Attribute)obj).getValue());
        } else if (String.class.equals(((Attribute)obj).getValue().getClass())) {
          setAttributeList.add(obj);
          valueMap.put(((Attribute)obj).getName(), new Double((String)((Attribute)obj).getValue()));
        }
      }
    }

    return setAttributeList;
  }

  /**
   * Operation invocation is not supported by this MBean.
   *
   * @param actionName the name of the operation to invoke
   * @param params     the operation parameters (ignored)
   * @param signature  the operation signature (ignored)
   * @return nothing; always throws
   * @throws UnsupportedOperationException always, since no operations are defined
   */
  @Override
  public Object invoke (String actionName, Object[] params, String[] signature) {

    throw new UnsupportedOperationException(actionName);
  }

  /**
   * Returns the static metadata that describes this MBean to the JMX infrastructure.
   *
   * @return the {@link MBeanInfo} built at construction time
   */
  @Override
  public MBeanInfo getMBeanInfo () {

    return mBeanInfo;
  }
}
