package nio.engine;

import java.nio.ByteBuffer;

public interface DeliverCallback {
  
  /**
   * Callback to notify that a message has been received.
   * The message is whole, all bytes have been accumulated.
   * @param channel
   * @param bytes
   */
  public void deliver(NioChannel channel, ByteBuffer bytes);
}
