package nio.engine;

/**
 * This class wraps an accepted connection.
 * It provides access to the port on which the connection was accepted
 * as well as the ability to close that  
 */
public abstract class NioServer {

  /**
   * @return the port onto which connections are accepted.
   */
  public abstract int getPort();
    
  /**
   * Close the server port, no longer accepting connections.
   */
  public abstract void close();

}
