package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class GBNServer {

  // UDP server
  private DatagramSocket server;
  // Receive container
  private byte[] container = new byte[1024];
  // pack the data into package
  private DatagramPacket packet = new DatagramPacket(container, container.length);
  private int window = 5;
  private int next = 0;
  private int base = 0;

  public GBNServer(int port) {
    try {
      this.server = new DatagramSocket(port);
    } catch (SocketException e) {
      // TODO 自动生成的 catch 块
      System.out.println("Failed to start the server!");
      e.printStackTrace();
    }
    System.out.println("Begin to receive message from client");

    // use receive method to get message from client
    // receive is a block method
    try {
      server.receive(packet);
    } catch (IOException e) {
      // TODO 自动生成的 catch 块
      System.out.println("Failed to receive message from client!");
      e.printStackTrace();
    }

    // get data from packet
    byte[] recevieData = this.packet.getData();
    int length = this.packet.getLength();
    String message = new String(recevieData, 0, length);
    System.out.println("Get message from client " + message);

    // send message to client
    System.out.println("Begin to send data to " + this.packet.getAddress());
    this.sendGBNMessage();

    // Set the timeout
    try {
      this.server.setSoTimeout(5000);
    } catch (SocketException e) {
      // TODO 自动生成的 catch 块
      System.out.println("Set timeout wrong!");
      e.printStackTrace();
    }

    while (true) {
      try {
        while (true) {
          server.receive(this.packet);

          // Receive ACKs from client
          recevieData = this.packet.getData();
          length = this.packet.getLength();
          message = new String(recevieData, 0, length);
          System.out.println("Receive message from client " + message);

          // get the ack
          String[] splitlist = message.split("\\s");
          int ACK = Integer.valueOf(splitlist[1]);
          System.out.println(ACK + ", " + base);
          if (ACK >= this.base) {
            this.base = ACK + 1;
            this.sendGBNMessage();
          } else {
            System.out.println("Error on receiving ACKs!!!");
          }
        }
      } catch (Exception e) {
        // TODO: handle exception
        System.out.println("Message " + this.base + " Timeout! Resending!");
        this.next = this.base;
        this.sendGBNMessage();
      }
    }

  }

  public static void main(String[] args) {
    int port = 12345;
    new GBNServer(port);
  }

  private void sendGBNMessage() {

    if (this.base > 100) {
      throw new RuntimeException("It's enough!");
    }
    while (this.next < this.base + this.window) {
      String sendData = "Seq " + this.next + "\r\n" + "Message " + this.next;
      byte[] sendByte = sendData.getBytes();
      // put data into the packet and send it to the client
      DatagramPacket sendPacket = new DatagramPacket(sendByte, sendByte.length,
          this.packet.getAddress(), this.packet.getPort());
      try {
        // send data to client
        this.server.send(sendPacket);
      } catch (IOException e) {
        System.out.println("Failed to send message to the client!");
        e.printStackTrace();
      }
      System.out.println("Send Message " + this.next + " to the client.");
      // send successfully, add the next count 1
      this.next += 1;
      try {
        // wait for sending time
        Thread.sleep(500);
      } catch (InterruptedException e) {
        // TODO 自动生成的 catch 块
        System.out.println("Sending has been Interrupted!");
        e.printStackTrace();
      }
    }
  }
}
