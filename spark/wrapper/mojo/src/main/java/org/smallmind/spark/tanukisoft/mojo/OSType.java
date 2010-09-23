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