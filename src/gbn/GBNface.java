/**
 * 
 */
package gbn;

import java.net.DatagramPacket;

/**
 * An Interface for GBNserver and GBNclient. Because they both can send and
 * receive message.
 * 
 * @author Raymo
 *
 */
public interface GBNface {

  /**
   * @param index
   */
  public void Timeout(int index);

  /**
   * @param index
   * @return
   */
  public DatagramPacket packDatagram(int index);

  /**
   * 
   */
  public void send();

  /**
   * 
   */
  public void Receive();
}
