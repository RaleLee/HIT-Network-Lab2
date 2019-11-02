package gbn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

public class GBNclient implements GBNface, Runnable {

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

  public GBNclient(InputStream fileInputStream, OutputStream fileOutputStream) {
    fileSaveStream = fileOutputStream;

    try {
      sendSocket = new DatagramSocket(RunGBN.clientPort);// 创建用于接受的socket

      byte data[] = new byte[datagramLength];
      int getlength = 0;
      while (-1 != (getlength = fileInputStream.read(data, dataheaderLength,
          datagramLength - dataheaderLength))) {
        end++;
        databyteList.add(Arrays.copyOf(data, getlength + dataheaderLength));//
      }
      System.out.println("receive:" + end);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void Timeout(int index) {
    if (isSend)
      System.out.println("reciver超时： " + index);
    if (send_base < index && isSend) {
      for (int i = send_base; i <= pos && i < end; i++) {
        try {
          sendSocket.send(packDatagram(i));
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
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
    System.out.println("receiver：发送" + (recv_base) + "的确认");
    return new DatagramPacket(databyteList.get(index), databyteList.get(index).length, aimAddress,
        RunGBN.serverPort);
  }

  public void send() {
    isSend = true;
    while (isSend) {
      if (aimAddress == null) {
        continue;
      }
      if (send_base + Nsize > pos && pos < end) {
        // printWindow();
        try {
          sendSocket.send(packDatagram(pos));
          new Thread(new Timer(pos, this)).start();
          pos++;
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      try {
        Thread.sleep(17);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

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

  private void recvPackethandle(DatagramPacket recvPacket) throws IOException {
    if (aimAddress == null) {
      aimAddress = recvPacket.getAddress();
    }
    int isEnd = recvPacket.getData()[0];
    int seq = recvPacket.getData()[1];
    System.out.println("receiver: 收到" + seq + "分组");
    int ack = recvPacket.getData()[2];
    int length = recvPacket.getData()[3];

    if (seq == recv_base + 1) {
      recv_base = Math.random() < 0.9 || isEnd == 1 ? recv_base + 1 : recv_base;
      if (seq == recv_base) {
        fileSaveStream.write(recvPacket.getData(), dataheaderLength, length);
        sendSocket.send(packDatagram(Math.min(recv_base, end - 1)));
      } else {
        System.out.println("人造丢包 seq= " + seq);
        return;
      }
    } else if (seq > recv_base + 1) {
      System.out.println("乱序丢弃 seq= " + seq);
      return;
    } // 模拟丢包

    if (1 == isEnd) {
      isRecv = false;
      System.out.println("receiver：接受完毕");
    }
    if (ack > send_base) {
      send_base = ack + 1;
    }
    if (ack == end - 1) {
      isSend = false;
      System.out.println("receiver：发送完毕");
    }

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
    private GBNclient gbnclient;

    public Timer(int index, GBNclient gbnclient) {
      this.index = index;
      this.gbnclient = gbnclient;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(5000);
        gbnclient.Timeout(index);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

    }
  }
}
