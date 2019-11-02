package sr;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

public class SRclient implements Runnable {

  static int recv_port = 12339;

  private int datagramLength = 120;// 报文长度
  private int dataheaderLength = 4;// 报文头部长度
  private Set<Integer> getSet = new HashSet<Integer>();
  private InetAddress aimAddress;// 发送目标地址
  private OutputStream filesaveStream;
  private DatagramSocket recvSocket;

  private boolean isRecv = false;

  public SRclient(OutputStream filesaveStream) {
    this.filesaveStream = filesaveStream;
    try {
      recvSocket = new DatagramSocket(RunSR.clientPort);
    } catch (SocketException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private DatagramPacket makeDatagramPacket(int index) {

    byte resp[] = new byte[4];
    resp[0] = 0;
    resp[1] = 0;
    resp[2] = (byte) index;
    resp[3] = 0;
    System.out.println("receiver：发送" + (index) + "的确认");
    return new DatagramPacket(resp, dataheaderLength, aimAddress, RunSR.serverPort);

  }

  private void receive() {
    isRecv = true;
    DatagramPacket recvPacket = new DatagramPacket(new byte[datagramLength], datagramLength);
    while (isRecv) {
      try {
        recvSocket.receive(recvPacket);
        recvPackethandle(recvPacket);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      try {
        Thread.sleep((int) (Math.random() * 17));
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private void recvPackethandle(DatagramPacket recvPacket) throws IOException {
    if (aimAddress == null) {
      aimAddress = recvPacket.getAddress();
    }
    recvPacket.getData();
    int seq = recvPacket.getData()[1];
    System.out.println("receiver: 收到" + seq + "分组");
    int end = recvPacket.getData()[2];
    int length = recvPacket.getData()[3];

    // 模拟丢包
    if (Math.random() > 0.9) {
      return;
    } else {
      getSet.add(seq);
      filesaveStream.write(recvPacket.getData(), dataheaderLength, length);
      System.out.println("成功接收分组" + seq);
      recvSocket.send(makeDatagramPacket(seq));
      if (getSet.size() == end) {
        isRecv = false;
      }
    }

  }

  @Override
  public void run() {
    receive();
  }

}
