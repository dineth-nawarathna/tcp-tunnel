package com.example.httptunnel;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClientApp {


  public static Socket tunnelServiceSocket = null;

  static String domainName = "dpn";
  static int sourcePort = 3000;

  static String serverIp = "localhost";

  public static void connectToTunnelService() {
    try {
      tunnelServiceSocket = new Socket(InetAddress.getByName(serverIp),
        Const.TUNNEL_SERVICE_PORT);

      ObjectOutputStream objectOutputStream = new ObjectOutputStream(tunnelServiceSocket.getOutputStream());
      objectOutputStream.writeObject(domainName);

      System.out.println("Connected!");


    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void listeningToTunnelService() {
    new Thread(() -> {
      try (InputStream inputStream = tunnelServiceSocket.getInputStream()) {
        while (true) {
          int read = inputStream.read();
          if (read == -1) {
            break;
          }
          switch (read) {
            case Const.RECONNECT:
              connect();
              break;
          }

        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();
  }


  private static void connect() {
    try {
      final Socket tunnelSocket = new Socket(InetAddress.getByName(serverIp),
        Const.TUNNEL_PORT);
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(tunnelSocket.getOutputStream());
      objectOutputStream.writeObject(domainName);
      new Thread(() -> {
        try {
          Socket redirectSocket = new Socket(InetAddress.getByName("localhost"),
            sourcePort);
          copySocketStreams(tunnelSocket,
            redirectSocket);
          copySocketStreams(redirectSocket,
            tunnelSocket);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    connectToTunnelService();
    listeningToTunnelService();
  }


  static void copySocketStreams(final Socket webSocket, final Socket appSocket) {
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
