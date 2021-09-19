package com.example.httptunnel;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

@Configuration
public class PrivateTrafficHandler {

  public static HashMap<String, Socket> TUNNEL_SERVICE_SESSIONS = new HashMap<>();
  public static HashMap<String, Socket> SESSIONS = new HashMap<>();

  @Bean
  void handleTunnelSessions() {
    new Thread(() -> {

      try {
        System.out.println("Listening to clients");
        ServerSocket privateSocket = new ServerSocket(Const.TUNNEL_PORT);
        while (true) {
          Socket socket = privateSocket.accept();
          ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
          String domain = objectInputStream.readObject().toString();
          SESSIONS.put(domain,
            socket);
          System.out.println("Client Connected ! - " + domain);
        }
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
    }).start();
  }

  @Bean
  void handleTunnelServiceSessions() {
    new Thread(() -> {
      try {
        System.out.println("Listening to client tunnels");
        ServerSocket privateSocket = new ServerSocket(Const.TUNNEL_SERVICE_PORT);
        while (true) {
          Socket socket = privateSocket.accept();
          ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
          String domain = objectInputStream.readObject().toString();
          TUNNEL_SERVICE_SESSIONS.put(domain,
            socket);
          System.out.println("Client Tunnel Connected! - "+domain);
        }
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
    }).start();
  }
}
