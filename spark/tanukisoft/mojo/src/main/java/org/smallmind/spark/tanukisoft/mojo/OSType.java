/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.spark.tanukisoft.mojo;

public enum OSType {

  AIX_PPC_32(OSStyle.UNIX, "wrapper-aix-ppc-32", "libwrapper-aix-ppc-32.a"),
  AIX_PPC_64(OSStyle.UNIX, "wrapper-aix-ppc-64", "libwrapper-aix-ppc-64.a"),
  HPUX_IA_32(OSStyle.UNIX, "wrapper-hpux-ia-32", "libwrapper-hpux-ia-32.so"),
  HPUX_IA_64(OSStyle.UNIX, "wrapper-hpux-ia-64", "libwrapper-hpux-ia-64.so"),
  HPUX_PARISC_32(OSStyle.UNIX, "wrapper-hpux-parisc-32", "libwrapper-hpux-parisc-32.sl"),
  HPUX_PARISC_64(OSStyle.UNIX, "wrapper-hpux-parisc-64", "libwrapper-hpux-parisc-64.sl"),
  LINUX_IA_64(OSStyle.UNIX, "wrapper-linux-ia-64", "libwrapper-linux-ia-64.so"),
  LINUX_PPC_32(OSStyle.UNIX, "wrapper-linux-ppc-32", "libwrapper-linux-ppc-32.so"),
  LINUX_PPC_64(OSStyle.UNIX, "wrapper-linux-ppc-64", "libwrapper-linux-ppc-64.so"),
  LINUX_X86_32(OSStyle.UNIX, "wrapper-linux-x86-32", "libwrapper-linux-x86-32.so"),
  LINUX_X86_64(OSStyle.UNIX, "wrapper-linux-x86-64", "libwrapper-linux-x86-64.so"),
  MACOSX_UNIVERSAL_32(OSStyle.UNIX, "wrapper-macosx-universal-32", "libwrapper-macosx-universal-32.jnilib"),
  MACOSX_UNIVERSAL_64(OSStyle.UNIX, "wrapper-macosx-universal-64", "libwrapper-macosx-universal-64.jnilib"),
  SOLARIS_SPARC_32(OSStyle.UNIX, "wrapper-solaris-sparc-32", "libwrapper-solaris-sparc-32.so"),
  SOLARIS_SPARC_64(OSStyle.UNIX, "wrapper-solaris-sparc-64", "libwrapper-solaris-sparc-64.so"),
  SOLARIS_X86_32(OSStyle.UNIX, "wrapper-solaris-x86-32", "libwrapper-solaris-x86-32.so"),
  SOLARIS_X86_64(OSStyle.UNIX, "wrapper-solaris-x86-64", "libwrapper-solaris-x86-64.so"),
  WINDOWS_X86_32(OSStyle.WINDOWS, "wrapper-windows-x86-32.exe", "wrapper-windows-x86-32.dll"),
  WINDOWS_X86_64(OSStyle.WINDOWS, "wrapper-windows-x86-64.exe", "wrapper-windows-x86-64.dll");

  private OSStyle osStyle;
  private String executable;
  private String library;

  private OSType (OSStyle osStyle, String executable, String library) {

    this.osStyle = osStyle;
    this.executable = executable;
    this.library = library;
  }

  public OSStyle getOsStyle () {

    return osStyle;
  }

  public String getExecutable () {

    return executable;
  }

  public String getLibrary () {

    return library;
  }
}