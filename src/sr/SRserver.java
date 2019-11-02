package sr;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SRserver implements Runnable {

  static int send_port = 12338;

  private int Nsize = 5;// 窗口大小
  private int send_base = 0;// 当前发送报文已确认（不包含）

  private int pos = 0;// 当前发送最后一个编号
  private int end = 0;// 最后一个报文编号
  private int datagramLength = 120;// 报文长度
  private int dataheaderLength = 4;// 报文头部长度

  private boolean isSend = false;

  ArrayList<byte[]> databyteList = new ArrayList<byte[]>();// 待发送数据
  Set<Integer> ackSet = new HashSet<Integer>();
  DatagramSocket sendSocket;// sender使用socket
  InetAddress aimAddress;// 发送目标地址

  public SRserver(InputStream fileInputStream, InetAddress aimIPAddress) {
    aimAddress = aimIPAddress;
    try {
      sendSocket = new DatagramSocket(RunSR.serverPort);// 创建用于发送的socket

      byte data[] = new byte[datagramLength];
      int getlength = 0;
      while (-1 != (getlength = fileInputStream.read(data, dataheaderLength,
          datagramLength - dataheaderLength))) {
        end++;
        databyteList.add(Arrays.copyOf(data, getlength + dataheaderLength));
      }
      System.out.println("send:" + end);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public SRserver() {
    // TODO 自动生成的构造函数存根
  }

  private DatagramPacket makeDatagramPacket(int index) {
    if (index == end - 1) {// 是否为最后一个分组
      databyteList.get(index)[0] = 1;
    } else {
      databyteList.get(index)[0] = 0;
    }
    databyteList.get(index)[1] = (byte) index;
    databyteList.get(index)[2] = (byte) end;
    databyteList.get(index)[3] = (byte) (databyteList.get(index).length - dataheaderLength);
    return new DatagramPacket(databyteList.get(index), databyteList.get(index).length, aimAddress,
        RunSR.clientPort);
  }

  private void printWindow() {
    System.out.print("sender: ");
    for (int i = 0; i < end; i++) {
      if (send_base == i) {
        System.out.print("[ ");
      }
      System.out.print(i + " ");
      if (ackSet.contains(i)) {
        System.out.print("√ ");
      } else {
        System.out.print("X ");
      }
      if (pos == i) {
        System.out.print("] ");
      }
    }
    System.out.println("  ");

  }

  private void send() {
    isSend = true;
    while (isSend) {
      if (send_base + Nsize > pos && pos < end) {
        try {
          printWindow();
          sendSocket.send(makeDatagramPacket(pos));
          new Thread(new Timer(pos, this)).start();
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

  private void receive() {
    DatagramPacket recvPacket = new DatagramPacket(new byte[datagramLength], datagramLength);
    while (isSend) {
      try {
        sendSocket.receive(recvPacket);
        recvPackethandle(recvPacket);
      } catch (IOException e) {
        e.printStackTrace();
      }

    }
  }

  private void recvPackethandle(DatagramPacket recvPacket) throws IOException {

    int ack = recvPacket.getData()[2];
    ackSet.add(ack);
    int min;
    for (min = 0; min <= end; min++) {
      if (!ackSet.contains(min)) {
        break;
      }
    }
    send_base = min;
    System.out.println("sender：收到" + ack + "的确认,pos = " + pos);
    if (ackSet.size() == end) {
      isSend = false;
      System.out.println("sender：发送完毕");
      printWindow();
    }

    // 模拟丢包

  }

  @Override
  public void run() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        receive();
      }
    }).start();

    send();

  }

  public void Timeout(int index) {
    try {
      if (!ackSet.contains(index)) {
        printWindow();
        System.out.println("重发" + index + "分组");
        sendSocket.send(makeDatagramPacket(index));
        new Thread(new Timer(index, this)).start();
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private class Timer implements Runnable {
    private int index;
    private SRserver srserver;
    
    public Timer(int index, SRserver srserver) {
      this.index = index;
      this.srserver = srserver;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(2000);
        srserver.Timeout(index);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }
  }
}
