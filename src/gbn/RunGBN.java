package gbn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;

public class RunGBN {

  public static int serverPort = 12345;
  public static int clientPort = 12346;

  public static void main(String[] args) throws Exception {
    File serverSend = new File("log/serverSend.txt");
    File clientSend = new File("log/clientSend.txt");
    File serverRece = new File("log/serverRece.txt");
    File clientRece = new File("log/clientRece.txt");

    FileInputStream serverSendStream = new FileInputStream(serverSend);
    FileOutputStream serverReceStream = new FileOutputStream(serverRece);
    FileInputStream clientSendStream = new FileInputStream(clientSend);
    FileOutputStream clientReceStream = new FileOutputStream(clientRece);

    new Thread(new GBNserver(serverSendStream, serverReceStream, InetAddress.getLocalHost()))
        .start();
    new Thread(new GBNclient(clientSendStream, clientReceStream)).start();

  }

}
