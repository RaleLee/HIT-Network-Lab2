package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {

  // UDP Client
  private DatagramSocket client;
  // Receive container
  private byte[] receive = new byte[1024];
  // pack the data into package
  private DatagramPacket packet;
  // Acks
  private int Ack = 0;

  public Client(int port) throws InterruptedException {
    try {
      client = new DatagramSocket();
    } catch (SocketException e) {
      // TODO 自动生成的 catch 块
      System.out.println("Failed to start the client!");
      e.printStackTrace();
    }
    System.out.println("Start the client successfully!");
    Thread.sleep(10000);
    String sendData = "Seq " + this.Ack + "\r\n" + "need data";
    byte[] sendByte = sendData.getBytes();
    // send to server
    try {
      this.packet = new DatagramPacket(sendByte, sendByte.length, InetAddress.getLocalHost(), port);
      client.send(this.packet);
    } catch (UnknownHostException e) {
      // TODO 自动生成的 catch 块
      System.out.println("Can't get local host!");
      e.printStackTrace();
    } catch (IOException e) {
      // TODO 自动生成的 catch 块
      System.out.println("Failed to send to server!");
      e.printStackTrace();
    }

    DatagramPacket fromServer = new DatagramPacket(this.receive, this.receive.length);

    // count for simulate missing acks
    int count = 0;
    while (true) {
      // receive message from server
      try {
        client.receive(fromServer);
      } catch (IOException e) {
        // TODO 自动生成的 catch 块
        System.out.println("Failed to receive message from server!");
        e.printStackTrace();
      }
      count += 1;
      byte[] recevieData = fromServer.getData();
      int length = fromServer.getLength();
      String message = new String(recevieData, 0, length);
      System.out.println("Get message from server " + message);

      String[] splitlist = message.split("\\s");
      int index = Integer.valueOf(splitlist[1]);
      System.out.println(index);
      if (this.Ack == index) {
        sendData = "ACK " + this.Ack + "\r\n";
        this.Ack += 1;
      } else {
        sendData = "ACK " + (this.Ack - 1) + "\r\n";
      }
      sendByte = sendData.getBytes();

//      System.out.println("Send to server " + sendData);
//      try {
//        this.packet = new DatagramPacket(sendByte, sendByte.length, InetAddress.getLocalHost(),
//            port);
//        this.client.send(packet);
//      } catch (UnknownHostException e) {
//        // TODO 自动生成的 catch 块
//        System.out.println("Can't get local host!");
//        e.printStackTrace();
//      } catch (IOException e) {
//        // TODO 自动生成的 catch 块
//        System.out.println("Failed to send to server!");
//        e.printStackTrace();
//      }
      // missing Acks
      if (count % 6 == 0) {
        System.out.println("Send to server " + sendData);
        try {
          this.packet = new DatagramPacket(sendByte, sendByte.length, InetAddress.getLocalHost(),
              port);
          this.client.send(packet);
        } catch (UnknownHostException e) {
          // TODO 自动生成的 catch 块
          System.out.println("Can't get local host!");
          e.printStackTrace();
        } catch (IOException e) {
          // TODO 自动生成的 catch 块
          System.out.println("Failed to send to server!");
          e.printStackTrace();
        }
      }
    }

  }

  public static void main(String[] args) throws InterruptedException {
    int port = 12345;
    new Client(port);
  }
}
