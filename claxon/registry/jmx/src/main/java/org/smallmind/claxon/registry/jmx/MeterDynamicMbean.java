package org.smallmind.claxon.registry.jmx;

import java.util.concurrent.ConcurrentHashMap;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

public class MeterDynamicMbean implements DynamicMBean {

  private ConcurrentHashMap<String, Double> valueMap = new ConcurrentHashMap<>();

  @Override
  public Object getAttribute (String attribute)
    throws AttributeNotFoundException, MBeanException, ReflectionException {

    Double value;

    if ((value = valueMap.get(attribute)) == null) {

    }

    return
  }

  @Override
  public void setAttribute (Attribute attribute)
    throws AttributeNotFoundException {

    throw new AttributeNotFoundException(attribute.getName());
  }

  @Override
  public AttributeList getAttributes (String[] attributes) {

    return null;
  }

  @Override
  public AttributeList setAttributes (AttributeList attributes) {

    return null;
  }

  @Override
  public Object invoke (String actionName, Object[] params, String[] signature)
    throws MBeanException, ReflectionException {

   throw new ReflectionException("No such ")
  }

  @Override
  public MBeanInfo getMBeanInfo () {

    return null;
  }
}
