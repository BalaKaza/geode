package com.gemstone.gemfire.distributed.internal.tcpserver;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gemstone.gemfire.DataSerializable;
import com.gemstone.gemfire.cache.GemFireCache;
import com.gemstone.gemfire.distributed.DistributedSystem;
import com.gemstone.gemfire.distributed.internal.PoolStatHelper;
import com.gemstone.gemfire.distributed.internal.SharedConfiguration;
import com.gemstone.gemfire.internal.AvailablePort;
//import com.gemstone.org.jgroups.stack.GossipClient;
//import com.gemstone.org.jgroups.stack.IpAddress;
import com.gemstone.gemfire.test.junit.categories.UnitTest;

@Category(UnitTest.class)
public class TcpServerJUnitTest {
  
  protected/*GemStoneAddition*/ InetAddress localhost;
  protected/*GemStoneAddition*/ int port;
  private SimpleStats stats;
  private TcpServer server;

  public void start(TcpHandler handler) throws IOException {
    localhost = InetAddress.getLocalHost();
    port = AvailablePort.getRandomAvailablePort(AvailablePort.SOCKET);
    
    stats = new SimpleStats();
    server = new TcpServer(port, localhost , new Properties(), null, handler, stats, Thread.currentThread().getThreadGroup(), "server thread");
    server.start();
  }
  
  @Test
  public void test() throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
    EchoHandler handler = new EchoHandler();
    start(handler);
    
    TestObject test = new TestObject();
    test.id = 5;
    TestObject result = (TestObject) TcpClient.requestToServer(localhost, port, test, 60 * 1000 );
    Assert.assertEquals(test.id, result.id);
    
    String[] info = TcpClient.getInfo(localhost, port);
    Assert.assertNotNull(info);
    Assert.assertTrue(info.length > 1);
   
    try { 
      TcpClient.stop(localhost, port);
    } catch ( ConnectException ignore ) {
      // must not be running 
    }
    server.join(60 * 1000);
    Assert.assertFalse(server.isAlive());
    Assert.assertTrue(handler.shutdown);
    
    Assert.assertEquals(4, stats.started.get());
    Assert.assertEquals(4, stats.ended.get());
    
  }
  
  @Test
  public void testConcurrency() throws UnknownHostException, IOException, ClassNotFoundException, InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    DelayHandler handler = new DelayHandler(latch);
    start(handler);
    
    final AtomicBoolean done = new AtomicBoolean();
    Thread delayedThread = new Thread() {
      public void run() {
        Boolean delay = Boolean.valueOf(true);
        try {
          TcpClient.requestToServer(localhost, port, delay, 60 * 1000 );
        } catch (IOException e) {
          e.printStackTrace();
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
        done.set(true);
      }
    };
    delayedThread.start();
    try {
      Thread.sleep(500);
      Assert.assertFalse(done.get());
      TcpClient.requestToServer(localhost, port, Boolean.valueOf(false), 60 * 1000 );
      Assert.assertFalse(done.get());

      latch.countDown();
      Thread.sleep(500);
      Assert.assertTrue(done.get());
    } finally {
      latch.countDown();
      delayedThread.join(60 * 1000);
      Assert.assertTrue(!delayedThread.isAlive()); // GemStoneAddition
      try {
        TcpClient.stop(localhost, port);
      } catch ( ConnectException ignore ) {
        // must not be running 
      }
      server.join(60 * 1000);
    }
  }
  
  public static class TestObject implements DataSerializable {
    int id;
    
    public TestObject() {
      
    }

    public void fromData(DataInput in) throws IOException {
      id = in.readInt();
    }

    public void toData(DataOutput out) throws IOException {
      out.writeInt(id);
    }
    
  }

  protected/*GemStoneAddition*/ static class EchoHandler implements TcpHandler {

    protected/*GemStoneAddition*/ boolean shutdown;


    public void init(TcpServer tcpServer) {
      // TODO Auto-generated method stub
      
    }

    public Object processRequest(Object request) throws IOException {
      return request;
    }

    public void shutDown() {
      shutdown = true;
    }
    
    public void restarting(DistributedSystem ds, GemFireCache cache, SharedConfiguration sharedConfig) { }
    public void endRequest(Object request,long startTime) { }
    public void endResponse(Object request,long startTime) { }
    
  }
  
  private static class DelayHandler implements TcpHandler {

    private CountDownLatch latch;

    public DelayHandler(CountDownLatch latch) {
      this.latch = latch;
    }

    public void init(TcpServer tcpServer) {
    }

    public Object processRequest(Object request) throws IOException {
      Boolean delay = (Boolean) request;
      if(delay.booleanValue()) {
        try {
          latch.await(120 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return delay;
      }
      else {
        return delay;
      }
    }

    public void shutDown() {
    }
    public void restarting(DistributedSystem ds, GemFireCache cache, SharedConfiguration sharedConfig) { }
    public void endRequest(Object request,long startTime) { }
    public void endResponse(Object request,long startTime) { }
  }
  
  protected/*GemStoneAddition*/ static class SimpleStats implements PoolStatHelper {
    AtomicInteger started = new AtomicInteger();
    AtomicInteger ended = new AtomicInteger();
    

    public void endJob() {
      started.incrementAndGet();
    }

    public void startJob() {
      ended.incrementAndGet();
    }
  }
}
