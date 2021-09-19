package com.example.httptunnel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

@Configuration
public class PublicTrafficHandler {

  @Bean
  void handlePublicRequests() {
    new Thread(() -> {
      try {
        ServerSocket publicSocket = new ServerSocket(8082);
        System.out.println("Started listening to public traffic");
        while (true) {

          Socket mainSocket = publicSocket.accept();

          String requestedDomain = "dpn";

          PrivateTrafficHandler.SESSIONS.remove(requestedDomain);
          Socket tunnelServiceSocket = PrivateTrafficHandler.TUNNEL_SERVICE_SESSIONS.get(requestedDomain);
          if (tunnelServiceSocket == null) {
            System.out.println("Not found!");
            OutputStream outputStream = mainSocket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeBytes("HTTP/1.1 404 OK\r\n");
            dataOutputStream.writeBytes("Content-Type: text/html\r\n\r\n");
            dataOutputStream.writeBytes("<html><head></head><body>Not Found</body></html>");
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();
            continue;
          }

          tunnelServiceSocket.getOutputStream().write(Const.RECONNECT);

          //Wait till reconnect, 1 Min time out
          long currentTimeMillis = System.currentTimeMillis() + 10000;
          while (PrivateTrafficHandler.SESSIONS.get(requestedDomain) == null) {
            if (currentTimeMillis < System.currentTimeMillis()) {
              break;
            }
          }

          Socket socket = PrivateTrafficHandler.SESSIONS.get(requestedDomain);

          if (socket != null) {
            copySocketStreams(mainSocket,
              socket);
            copySocketStreams(socket,
              mainSocket);
          } else {
            System.out.println("Time out");
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }).start();
  }

  void copySocketStreams(final Socket webSocket, final Socket appSocket) {
    new Thread(() -> {
      InputStream inputStream = null;
      OutputStream outputStream = null;
      try {
        inputStream = webSocket.getInputStream();
        outputStream = appSocket.getOutputStream();
        Utils.copyStreams(inputStream,
          outputStream);
      } catch (SocketException e) {
        System.out.println("Socket Disconnected!");
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        if (inputStream != null) {
          try {
            inputStream.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (outputStream != null) {
          try {
            outputStream.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        try {
          webSocket.close();
          appSocket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }
}
