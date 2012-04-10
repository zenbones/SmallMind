/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.constellation.component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;
import org.smallmind.nutsnbolts.naming.ContextUtilities;
import org.smallmind.quorum.pool.connection.ConnectionPool;
import org.smallmind.quorum.pool.connection.ConnectionPool;
import org.smallmind.quorum.pool.connection.ConnectionPoolException;
import org.smallmind.quorum.pool.connection.ConnectionPoolException;
import org.smallmind.scribe.pen.Logger;

public abstract class SmallMindComponent {

  private final LinkedList<String> namespaceContextList;

  private ConnectionPool contextPool;
  private InetAddress hostAddress;
  private String hostName;

  public SmallMindComponent (ConnectionPool contextPool)
    throws UnknownHostException {

    this.contextPool = contextPool;

    hostAddress = InetAddress.getLocalHost();
    hostName = hostAddress.getCanonicalHostName();

    namespaceContextList = new LinkedList<String>();
  }

  public abstract String getComponentType ();

  public abstract Logger getLogger ();

  public abstract String getGlobalNamespacePath ();

  public abstract String getLocalNamespacePath ();

  public InetAddress getHostAddress () {

    return hostAddress;
  }

  public String getHostName () {

    return hostName;
  }

  private String completeNamespacePath (String basePath, String storageSubpath) {

    StringBuilder pathBuilder = new StringBuilder();

    pathBuilder.append("data/");
    pathBuilder.append(getComponentType());
    pathBuilder.append('/');
    pathBuilder.append(basePath);
    pathBuilder.append('/');
    pathBuilder.append(storageSubpath);

    return pathBuilder.toString();
  }

  private void insureNamespaceContext (DirContext smallmindEnvironment, String completePath)
    throws NamingException {

    synchronized (namespaceContextList) {
      if (!namespaceContextList.contains(completePath)) {
        ContextUtilities.ensureContext(smallmindEnvironment, completePath);
        namespaceContextList.add(completePath);
      }
    }
  }

  public void removeGlobalNamespaceContext (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    removeNamespaceContext(getGlobalNamespacePath(), storageSubpath);
  }

  public void removeLocalNamespaceContext (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    removeNamespaceContext(getLocalNamespacePath(), storageSubpath);
  }

  private void removeNamespaceContext (String basePath, String storageSubpath)
    throws ConnectionPoolException, NamingException {

    DirContext smallmindEnvironment;
    String completePath;

    completePath = completeNamespacePath(basePath, storageSubpath);

    synchronized (namespaceContextList) {
      smallmindEnvironment = (DirContext)contextPool.getConnection();
      try {
        smallmindEnvironment.destroySubcontext(completePath);
        namespaceContextList.remove(completePath);
      }
      finally {
        smallmindEnvironment.close();
      }
    }
  }

  public void setGlobalData (String storageSubpath, Object data)
    throws ConnectionPoolException, NamingException {

    setData(getGlobalNamespacePath(), storageSubpath, data);
  }

  public void setLocalData (String storageSubpath, Object data)
    throws ConnectionPoolException, NamingException {

    setData(getLocalNamespacePath(), storageSubpath, data);
  }

  private void setData (String basePath, String storageSubpath, Object data)
    throws ConnectionPoolException, NamingException {

    DirContext smallmindEnvironment;
    String completePath;

    if (data == null) {
      throw new IllegalArgumentException("Object data for storage must not be 'null'");
    }

    completePath = completeNamespacePath(basePath, storageSubpath);

    smallmindEnvironment = (DirContext)contextPool.getConnection();
    try {
      insureNamespaceContext(smallmindEnvironment, completePath);
      smallmindEnvironment.rebind(completePath, data);
    }
    finally {
      smallmindEnvironment.close();
    }
  }

  public Object getGlobalData (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    return getData(getGlobalNamespacePath(), storageSubpath);
  }

  public Object getLocalData (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    return getData(getLocalNamespacePath(), storageSubpath);
  }

  private Object getData (String basePath, String storageSubpath)
    throws ConnectionPoolException, NamingException {

    DirContext smallmindEnvironment;
    Object data;

    smallmindEnvironment = (DirContext)contextPool.getConnection();
    try {
      data = smallmindEnvironment.lookup(completeNamespacePath(basePath, storageSubpath));
    }
    catch (NameNotFoundException n) {
      return null;
    }
    finally {
      smallmindEnvironment.close();
    }

    return data;
  }

  public NamingEnumeration<NameClassPair> getGlobalList (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    return getList(getGlobalNamespacePath(), storageSubpath);
  }

  public NamingEnumeration<NameClassPair> getLocalList (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    return getList(getLocalNamespacePath(), storageSubpath);
  }

  private NamingEnumeration<NameClassPair> getList (String basePath, String storageSubpath)
    throws ConnectionPoolException, NamingException {

    DirContext smallmindEnvironment;

    smallmindEnvironment = (DirContext)contextPool.getConnection();
    try {
      return smallmindEnvironment.list(completeNamespacePath(basePath, storageSubpath));
    }
    finally {
      smallmindEnvironment.close();
    }
  }

  public void removeGlobalData (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    removeData(getGlobalNamespacePath(), storageSubpath);
  }

  public void removeLocalData (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    removeData(getLocalNamespacePath(), storageSubpath);
  }

  private void removeData (String basePath, String storageSubpath)
    throws ConnectionPoolException, NamingException {

    DirContext smallmindEnvironment;

    smallmindEnvironment = (DirContext)contextPool.getConnection();
    try {
      smallmindEnvironment.unbind(completeNamespacePath(basePath, storageSubpath));
    }
    finally {
      smallmindEnvironment.close();
    }
  }

  public Map<String, Object> getGlobalDataMap (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    return getDataMap(getGlobalNamespacePath(), storageSubpath);
  }

  public Map<String, Object> getLocalDataMap (String storageSubpath)
    throws ConnectionPoolException, NamingException {

    return getDataMap(getLocalNamespacePath(), storageSubpath);
  }

  private Map<String, Object> getDataMap (String basePath, String storageSubpath)
    throws ConnectionPoolException, NamingException {

    HashMap<String, Object> bindingMap;
    DirContext smallmindEnvironment;
    NamingEnumeration<Binding> bindingEnum;
    Binding binding;

    bindingMap = new HashMap<String, Object>();
    smallmindEnvironment = (DirContext)contextPool.getConnection();
    try {
      bindingEnum = smallmindEnvironment.listBindings(completeNamespacePath(basePath, storageSubpath));
      while (bindingEnum.hasMore()) {
        binding = bindingEnum.next();
        bindingMap.put(binding.getName(), binding.getObject());
      }
    }
    finally {
      smallmindEnvironment.close();
    }

    return bindingMap;
  }

  public Attributes getGlobalAttributes (String storageSubpath, String[] returnAttributeArray)
    throws ConnectionPoolException, NamingException {

    return getAttributes(getGlobalNamespacePath(), storageSubpath, returnAttributeArray);
  }

  public Attributes getLocalAttributes (String storageSubpath, String[] returnAttributeArray)
    throws ConnectionPoolException, NamingException {

    return getAttributes(getLocalNamespacePath(), storageSubpath, returnAttributeArray);
  }

  private Attributes getAttributes (String basePath, String storageSubpath, String[] returnAttributeArray)
    throws ConnectionPoolException, NamingException {

    DirContext smallmindEnvironment;

    smallmindEnvironment = (DirContext)contextPool.getConnection();
    try {
      return smallmindEnvironment.getAttributes(completeNamespacePath(basePath, storageSubpath), returnAttributeArray);
    }
    finally {
      smallmindEnvironment.close();
    }
  }

  public void modifyGlobalAttributes (String storageSubpath, ModificationItem[] modItems)
    throws ConnectionPoolException, NamingException {

    modifyAttributes(getGlobalNamespacePath(), storageSubpath, modItems);
  }

  public void modifyLocalAttributes (String storageSubpath, ModificationItem[] modItems)
    throws ConnectionPoolException, NamingException {

    modifyAttributes(getLocalNamespacePath(), storageSubpath, modItems);
  }

  private void modifyAttributes (String basePath, String storageSubpath, ModificationItem[] modItems)
    throws ConnectionPoolException, NamingException {

    DirContext smallmindEnvironment;
    String completePath;

    completePath = completeNamespacePath(basePath, storageSubpath);

    smallmindEnvironment = (DirContext)contextPool.getConnection();
    try {
      insureNamespaceContext(smallmindEnvironment, completePath);
      smallmindEnvironment.modifyAttributes(completePath, modItems);
    }
    finally {
      smallmindEnvironment.close();
    }
  }

  public List<Attributes> searchGlobalAttributes (String storageSubpath, Attributes searchAttributes, String[] returnAttributeArray)
    throws ConnectionPoolException, NamingException {

    return searchAttributes(getGlobalNamespacePath(), storageSubpath, searchAttributes, returnAttributeArray);
  }

  public List<Attributes> searchLocalAttributes (String storageSubpath, Attributes searchAttributes, String[] returnAttributeArray)
    throws ConnectionPoolException, NamingException {

    return searchAttributes(getLocalNamespacePath(), storageSubpath, searchAttributes, returnAttributeArray);
  }

  private List<Attributes> searchAttributes (String basePath, String storageSubpath, Attributes searchAttributes, String[] returnAttributeArray)
    throws ConnectionPoolException, NamingException {

    LinkedList<Attributes> resultList;
    DirContext smallmindEnvironment;
    NamingEnumeration<SearchResult> searchEnum;
    String completePath;

    completePath = completeNamespacePath(basePath, storageSubpath);

    resultList = new LinkedList<Attributes>();
    smallmindEnvironment = (DirContext)contextPool.getConnection();
    try {
      insureNamespaceContext(smallmindEnvironment, completePath);
      searchEnum = smallmindEnvironment.search(completeNamespacePath(basePath, storageSubpath), searchAttributes, returnAttributeArray);
      while (searchEnum.hasMore()) {
        resultList.add((searchEnum.next()).getAttributes());
      }
    }
    finally {
      smallmindEnvironment.close();
    }

    return resultList;
  }

  public void logInfo (String message, Object... args) {

    getLogger().info(message, args);
  }

  public void logWarning (Throwable throwable, String message, Object... args) {

    getLogger().warn(throwable, message, args);
  }

  public void logError (String message, Object... args) {

    getLogger().error(message, args);
  }

  public void logError (Throwable throwable, String message, Object... args) {

    getLogger().error(throwable, message, args);
  }

  public void logError (Throwable throwable) {

    getLogger().error(throwable);
  }
}
