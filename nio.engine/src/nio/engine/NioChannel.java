package nio.engine;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * This class wraps an end-point of a channel.
 * It allows to send and receive messages, 
 * stored in ByteBuffers. 
 */
public abstract class NioChannel {

  /**
   * Get access to the underlying socket channel.
   * @return
   */
  public abstract SocketChannel getChannel();

  /**
   * Set the callback to deliver messages to.
   * @param callback
   */
  public abstract void setDeliverCallback(DeliverCallback callback);

  /**
   * Get the Inet socket address for the other side of this channel.
   * @return
   */
  public abstract InetSocketAddress getRemoteAddress();

  /**
   * Send the given byte buffer. No copy is made, so the buffer 
   * should no longer be used by the code sending it.
   * @param buf
   */
  public abstract void send(ByteBuffer buf);

  /**
   * Sending the given byte array, a copy is made into internal buffers,
   * so the array can be reused after sending it.
   * @param bytes
   * @param offset
   * @param length
   */
  public abstract void send(byte[] bytes, int offset, int length);

  
  public abstract void close();

}
