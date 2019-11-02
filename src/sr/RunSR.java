package sr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;

public class RunSR {

  public static int serverPort = 12347;
  public static int clientPort = 12348;

  public static void main(String[] args) throws Exception {
    File serverSend = new File("log/SRSend.txt");
    File clientRece = new File("log/SRRece.txt");

    FileInputStream serverSendStream = new FileInputStream(serverSend);
    FileOutputStream clientReceStream = new FileOutputStream(clientRece);

    new Thread(new SRserver(serverSendStream, InetAddress.getLocalHost())).start();
    new Thread(new SRclient(clientReceStream)).start();

  }

}
