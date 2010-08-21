package org.smallmind.nutsnbolts.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.smallmind.nutsnbolts.io.FileIterator;

public class ZipUtilities {

   public static void compressDirectory (File directory, OutputStream outputStream)
      throws IOException {

      ZipOutputStream zipOutputStream;
      byte[] buffer = new byte[8192];

      zipOutputStream = new ZipOutputStream(outputStream);
      for (File file : new FileIterator(directory)) {

         FileInputStream fileInputStream;
         int bytesRead;

         fileInputStream = new FileInputStream(file);
         zipOutputStream.putNextEntry(new ZipEntry(file.getCanonicalPath()));

         while ((bytesRead = fileInputStream.read(buffer)) > 0) {
            zipOutputStream.write(buffer, 0, bytesRead);
         }

         zipOutputStream.closeEntry();
         fileInputStream.close();
      }

      zipOutputStream.close();
   }
}
