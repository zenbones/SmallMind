package org.smallmind.nagios;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.CRC32;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class NSCA {

   private static final int DEFAULT_SOCKET_TIMEOUT = 10000;
   private static final int INITIALISATION_VECTOR_SIZE = 128;
   private static final int PLUGIN_OUTPUT_SIZE = 512;
   private static final int HOST_NAME_SIZE = 64;
   private static final int SERVICE_NAME_SIZE = 128;
   private static final short NSCA_VERSION = 3;

   private EncryptionMethod encryptionMethod;
   private String host;
   private String password;
   private int port;
   int socketTimeout;

   public NSCA () {

      this("localhost", 5667, DEFAULT_SOCKET_TIMEOUT, null, EncryptionMethod.NONE);
   }

   public NSCA (String host, int port) {

      this(host, port, DEFAULT_SOCKET_TIMEOUT, null, EncryptionMethod.NONE);
   }

   public NSCA (String host, int port, int socketTimeout) {

      this(host, port, socketTimeout, null, EncryptionMethod.NONE);
   }

   public NSCA (String host, int port, String password, EncryptionMethod encryptionMethod) {

      this(host, port, DEFAULT_SOCKET_TIMEOUT, password, encryptionMethod);
   }

   public NSCA (String host, int port, int socketTimeout, String password, EncryptionMethod encryptionMethod) {

      this.host = host;
      this.port = port;
      this.socketTimeout = socketTimeout;
      this.password = password;
      this.encryptionMethod = encryptionMethod;
   }

   public void send (String serviceName, String message, ReturnCode returnCode) throws NagiosException {

      Socket socket;
      OutputStream outputStream;
      DataInputStream inputStream;
      NagiosException sendException = null;

      try {
         socket = new Socket(host, port);
         socket.setSoTimeout(socketTimeout);
         outputStream = socket.getOutputStream();
         inputStream = new DataInputStream(socket.getInputStream());
      }
      catch (IOException ioException) {
         throw new NagiosException(ioException);
      }

      try {

         int timeStamp;
         byte[] initVector = new byte[INITIALISATION_VECTOR_SIZE];

         try {
            inputStream.readFully(initVector, 0, INITIALISATION_VECTOR_SIZE);
            timeStamp = inputStream.readInt();
         }
         catch (IOException ioException) {
            throw new NagiosException("Encountered a problem in reading the initialization vector", ioException);
         }

         byte[] passiveCheckBytes = new byte[16 + HOST_NAME_SIZE + SERVICE_NAME_SIZE + PLUGIN_OUTPUT_SIZE];

         // 1st part, the NSCA version
         ByteUtilities.writeShort(passiveCheckBytes, NSCA_VERSION, 0);

         // 3rd part, echo back the time
         ByteUtilities.writeInteger(passiveCheckBytes, timeStamp, 8);

         // 4th part, the return code
         ByteUtilities.writeShort(passiveCheckBytes, (short)returnCode.getCode(), 12);

         // set up main payload
         int myOffset = 14;
         ByteUtilities.writeFixedString(passiveCheckBytes, host, myOffset, HOST_NAME_SIZE);
         ByteUtilities.writeFixedString(passiveCheckBytes, serviceName, myOffset += HOST_NAME_SIZE, SERVICE_NAME_SIZE);
         ByteUtilities.writeFixedString(passiveCheckBytes, message, myOffset += SERVICE_NAME_SIZE, PLUGIN_OUTPUT_SIZE);

         // 2nd part, CRC
         writeCRC(passiveCheckBytes);

         encryptPayloadUsingXOR(passiveCheckBytes, initVector);

         outputStream.write(passiveCheckBytes, 0, passiveCheckBytes.length);
         outputStream.flush();
      }
      catch (NagiosException nagiosException) {
         sendException = nagiosException;
      }
      catch (IOException ioException) {
         sendException = new NagiosException("Error occured while sending a passive alert", ioException);
      }
      finally {
         try {
            outputStream.close();
            inputStream.close();
            socket.close();
         }
         catch (IOException ioException) {
            if (sendException != null) {

               NagiosException originalException = sendException;

               sendException = new NagiosException(ioException);
               sendException.initCause(originalException);
            }
            else {
               sendException = new NagiosException(ioException);
            }
         }
      }

      if (sendException != null) {
         throw sendException;
      }
   }

   private void writeCRC (byte[] passiveCheckBytes) {

      CRC32 crc = new CRC32();

      crc.update(passiveCheckBytes);
      ByteUtilities.writeInteger(passiveCheckBytes, (int)crc.getValue(), 4);
   }

   private void encryptPayloadUsingXOR (byte[] sendBuffer, byte[] initVector) {

      switch (encryptionMethod) {
         case NONE:
            break;
         case XOR:
            for (int y = 0, x = 0; y < sendBuffer.length; y++, x++) {
               if (x >= INITIALISATION_VECTOR_SIZE) {
                  x = 0;
               }
               sendBuffer[y] ^= initVector[x];
            }

            if (password != null) {
               final byte[] passwordBytes = password.getBytes();

               for (int y = 0, x = 0; y < sendBuffer.length; y++, x++) {
                  if (x >= passwordBytes.length) {
                     x = 0;
                  }
                  sendBuffer[y] ^= passwordBytes[x];
               }
            }
            break;
         default:
            throw new UnknownSwitchCaseException(encryptionMethod.name());
      }

   }

   public static void main (String[] args)
      throws Exception {

      System.out.println("creating instance of NSCA ...");

      NSCA nsca = new NSCA();
      nsca.send("MyApplication", "something bad just happened", ReturnCode.WARN);
      System.out.println("send complete!!");
   }
}