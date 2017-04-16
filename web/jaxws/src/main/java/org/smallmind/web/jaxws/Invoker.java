/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.web.jaxws;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Invoker {

  public static void main (String... args) throws Exception {

    int idx = 1;
    String c = args[2];
    if ("-pathfile".equals(args[1])) {
      Properties p = new Properties();
      File pathFile = new File(args[2]);
      pathFile.deleteOnExit();
      p.load(new FileInputStream(pathFile));
      c = p.getProperty("cp");
      idx = 3;
    }
    List<URL> cp = new ArrayList<URL>();
    for (String s : c.split(File.pathSeparator)) {
      try {
        URL f = new File(s).toURI().toURL();
        cp.add(f);
      }
      catch (MalformedURLException ex) {
        Logger.getLogger(Invoker.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    URLClassLoader cl = new URLClassLoader(cp.toArray(new URL[cp.size()]));
    String[] wsargs = new String[args.length - idx];
    System.arraycopy(args, idx, wsargs, 0, args.length - idx);
    ClassLoader orig = Thread.currentThread().getContextClassLoader();
    String origJcp = System.getProperty("java.class.path");
    Thread.currentThread().setContextClassLoader(cl);
    System.setProperty("java.class.path", c);
    try {
      Class<?> compileTool = cl.loadClass(args[0]);
      Constructor<?> ctor = compileTool.getConstructor(OutputStream.class);
      Object tool = ctor.newInstance(System.out);
      Method runMethod = compileTool.getMethod("run", String[].class);
      System.exit((Boolean)runMethod.invoke(tool, new Object[] {wsargs}) ? 0 : 1);
    }
    catch (NoSuchMethodException ex) {
      Logger.getLogger(Invoker.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (SecurityException ex) {
      Logger.getLogger(Invoker.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (ClassNotFoundException ex) {
      Logger.getLogger(Invoker.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (InstantiationException ex) {
      Logger.getLogger(Invoker.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (IllegalAccessException ex) {
      Logger.getLogger(Invoker.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (IllegalArgumentException ex) {
      Logger.getLogger(Invoker.class.getName()).log(Level.SEVERE, null, ex);
    }
    catch (InvocationTargetException ex) {
      Exception rex = new RuntimeException();
      rex.initCause(ex);
      throw ex;
    }
    finally {
      Thread.currentThread().setContextClassLoader(orig);
      System.setProperty("java.class.path", origJcp);
    }
  }
}