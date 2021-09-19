package com.example.httptunnel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

public class Utils {

  public static void copyStreams(InputStream source, OutputStream target) throws IOException {


    byte[] buf = new byte[8192];
    int length;
    while ((length = source.read(buf)) > 0) {
      target.write(buf,
        0,
        length);
      target.flush();
    }

  }
}
