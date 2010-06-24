package org.smallmind.nutsnbolts.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.smallmind.nutsnbolts.util.Tuple;

public class HttpTransmitter {

   public static void emitGet (String cgiUrlString)
      throws IOException {

      emitGet(cgiUrlString, null);
   }

   public static void emitGet (String cgiUrlString, PrintWriter out)
      throws IOException {

      emitHttpRequest(cgiUrlString, null, out);
   }

   public static void emitPost (String cgiUrlString, Tuple<String, String> tuple)
      throws IOException {

      emitPost(cgiUrlString, tuple, null);
   }

   public static void emitPost (String cgiUrlString, Tuple<String, String> tuple, PrintWriter out)
      throws IOException {

      emitHttpRequest(cgiUrlString, tuple, out);
   }

   private static void emitHttpRequest (String cgiUrlString, Tuple<String, String> tuple, PrintWriter out)
      throws IOException {

      URL cgiUrl;
      HttpURLConnection cgiConnect;
      InputStream postInputStream;
      BufferedReader postReader;
      OutputStream postOutputStream;
      BufferedWriter postWriter;
      String encodedPostData = null;
      String singleLine;

      cgiUrl = new URL(cgiUrlString);
      cgiConnect = (HttpURLConnection)cgiUrl.openConnection();
      if (tuple != null) {
         encodedPostData = HTTPCodec.urlEncode(tuple);
         cgiConnect.setDoOutput(true);
         cgiConnect.setRequestMethod("POST");
      }
      else {
         cgiConnect.setDoOutput(false);
         cgiConnect.setRequestMethod("GET");
      }
      if (out != null) {
         cgiConnect.setDoInput(true);
      }
      else {
         cgiConnect.setDoInput(false);
      }
      cgiConnect.connect();
      if (tuple != null) {
         postOutputStream = cgiConnect.getOutputStream();
         postWriter = new BufferedWriter(new OutputStreamWriter(postOutputStream));
         postWriter.write(encodedPostData);
         postWriter.flush();
         postWriter.close();
      }
      if (out != null) {
         postInputStream = cgiConnect.getInputStream();
         postReader = new BufferedReader(new InputStreamReader(postInputStream));
         while ((singleLine = postReader.readLine()) != null) {
            out.println(singleLine);
         }
         postReader.close();
      }
      cgiConnect.disconnect();
   }

}
