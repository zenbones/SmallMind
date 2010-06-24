package org.smallmind.nutsnbolts.lang;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClasspathClassGate implements ClassGate {

   private final HashMap<String, String> filePathMap;

   private String[] pathComponents;

   public ClasspathClassGate () {

      this(System.getProperty("java.class.path"));
   }

   public ClasspathClassGate (String classPath) {

      this(classPath.split(System.getProperty("path.separator"), -1));
   }

   public ClasspathClassGate (String[] pathComponents) {

      this.pathComponents = pathComponents;

      filePathMap = new HashMap<String, String>();
   }

   public long getLastModDate (String name) {

      File classFile;
      String filePath;

      synchronized (filePathMap) {
         if ((filePath = filePathMap.get(name)) != null) {
            classFile = new File(filePath);

            return classFile.lastModified();
         }
      }

      return ClassGate.STATIC_CLASS;
   }

   public ClassStreamTicket getClassAsTicket (String name)
      throws Exception {

      String classFileName;

      classFileName = name.replace('.', '/') + ".class";

      for (String pathComponent : pathComponents) {

         InputStream classStream;

         if (pathComponent.endsWith(".jar")) {
            if ((classStream = findJarStream(pathComponent, classFileName)) != null) {
               return new ClassStreamTicket(classStream, ClassGate.STATIC_CLASS);
            }
         }
         else {

            File classFile;
            long timeStamp;

            if ((classFile = findFile(pathComponent, classFileName)) != null) {
               synchronized (filePathMap) {
                  filePathMap.put(name, classFile.getAbsolutePath());
                  timeStamp = classFile.lastModified();
                  return new ClassStreamTicket(new BufferedInputStream(new FileInputStream(classFile)), timeStamp);
               }
            }
         }
      }

      return null;
   }

   public InputStream getResourceAsStream (String path)
      throws Exception {

      for (String pathComponent : pathComponents) {

         InputStream resourceStream;

         if (pathComponent.endsWith(".jar")) {
            if ((resourceStream = findJarStream(pathComponent, path)) != null) {
               return resourceStream;
            }
         }
         else {

            File resourceFile;

            if ((resourceFile = findFile(pathComponent, path)) != null) {
               return new BufferedInputStream(new FileInputStream(resourceFile));
            }
         }
      }

      return null;
   }

   private InputStream findJarStream (String jarComponentPath, String path)
      throws IOException {

      JarFile jarFile;
      JarEntry jarEntry;
      Enumeration<JarEntry> entryEnumeration;

      jarFile = new JarFile(jarComponentPath);
      entryEnumeration = jarFile.entries();
      while (entryEnumeration.hasMoreElements()) {
         if ((jarEntry = entryEnumeration.nextElement()).getName().equals(path)) {
            return new BufferedInputStream(jarFile.getInputStream(jarFile.getEntry(jarEntry.getName())));
         }
      }

      return null;
   }

   private File findFile (String fileComponentPath, String path) {

      File pathFile;

      pathFile = new File(fileComponentPath + "/" + path);
      if (pathFile.isFile()) {

         return pathFile;
      }

      return null;
   }
}
