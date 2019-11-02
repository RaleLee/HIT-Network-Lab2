/**
 * 
 */
package gbn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Raymo
 *
 */
public class GBNserver implements GBNface, Runnable {
  private int Nsize = 5;// 窗口大小
  private int send_base = 0;// 当前发送报文已确认（不包含）
  private int recv_base = -1;// 当前接受已确认（包含）
  private int pos = 0;// 当前发送最后一个编号
  private int end = 0;// 最后一个报文编号
  private int datagramLength = 120;// 报文长度
  private int dataheaderLength = 4;// 报文头部长度

  private boolean isSend = false;
  private boolean isRecv = false;

  ArrayList<byte[]> databyteList = new ArrayList<byte[]>();// 待发送数据

  DatagramSocket sendSocket;// sender使用socket
  InetAddress aimAddress;// 发送目标地址

  OutputStream fileSaveStream;

  public GBNserver(InputStream fileInputStream, OutputStream fileOutputStream,
      InetAddress aimIPAddress) {
    fileSaveStream = fileOutputStream;
    aimAddress = aimIPAddress;
    try {
      sendSocket = new DatagramSocket(RunGBN.serverPort);// 创建用于发送的socket

      byte data[] = new byte[datagramLength];
      int getlength = 0;
      while (-1 != (getlength = fileInputStream.read(data, dataheaderLength,
          datagramLength - dataheaderLength))) {
        end++;
        databyteList.add(Arrays.copyOf(data, getlength + dataheaderLength));//
      }
      System.out.println("send:" + end);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void Timeout(int index) {
    // if(isSend) System.out.println("sender 超时： "+index);
    if (send_base <= index && isSend) {
      System.out.println("sender 超时重传 base = " + send_base);
      printWindow();
      for (int i = send_base; i <= pos && i < end; i++) {
        try {
          sendSocket.send(packDatagram(i));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      new Thread(new Timer(send_base, this)).start();
    }
  }

  public DatagramPacket packDatagram(int index) {
    if (index == end - 1) {// 是否为最后一个分组
      databyteList.get(index)[0] = 1;
    } else {
      databyteList.get(index)[0] = 0;
    }
    databyteList.get(index)[1] = (byte) index;
    databyteList.get(index)[2] = (byte) recv_base;
    databyteList.get(index)[3] = (byte) (databyteList.get(index).length - dataheaderLength);
    return new DatagramPacket(databyteList.get(index), databyteList.get(index).length, aimAddress,
        RunGBN.clientPort);
  }

  @Override
  public void send() {
    isSend = true;
    while (isSend) {
      if (send_base + Nsize > pos && pos < end) {
        try {
          printWindow();
          sendSocket.send(packDatagram(pos));
          new Thread(new Timer(send_base, this)).start();
          pos++;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Override
  public void Receive() {
    isRecv = true;
    DatagramPacket recvPacket = new DatagramPacket(new byte[datagramLength], datagramLength);
    while (isRecv || isSend) {
      try {
        sendSocket.receive(recvPacket);
        recvPackethandle(recvPacket);
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
  }

  private void printWindow() {
    System.out.print("sender: ");
    for (int i = 0; i < end; i++) {
      if (send_base == i) {
        System.out.print("[ ");
      }
      System.out.print(i + " ");
      if (pos == i) {
        System.out.print("] ");
      }
    }
    System.out.println("  ");
  }

  private void recvPackethandle(DatagramPacket recvPacket) throws IOException {
    int isEnd = recvPacket.getData()[0];
    int seq = recvPacket.getData()[1];
    int ack = recvPacket.getData()[2];
    int length = recvPacket.getData()[3];
    if (1 == isEnd) {
      isRecv = false;
      System.out.println("sender：接受完毕");
    }
    if (ack > send_base) {
      send_base = ack + 1;
      System.out.println("sender：收到" + ack + "的确认,pos = " + pos);
    }
    if (ack == end - 1) {
      isSend = false;
      System.out.println("sender：发送完毕");
    }
    if (seq == recv_base + 1) {
      recv_base = recv_base + 1;
      if (seq == recv_base) {
        fileSaveStream.write(recvPacket.getData(), dataheaderLength, length);
        sendSocket.send(packDatagram(Math.min(recv_base, end - 1)));
      }
    }
    // 模拟丢包

  }

  @Override
  public void run() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Receive();
      }
    }).start();

    send();
  }

  private class Timer implements Runnable {
    private int index;
    private GBNserver gbnserver;

    public Timer(int index, GBNserver gbnserver) {
      this.index = index;
      this.gbnserver = gbnserver;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(5000);
        gbnserver.Timeout(index);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }
  }
}
