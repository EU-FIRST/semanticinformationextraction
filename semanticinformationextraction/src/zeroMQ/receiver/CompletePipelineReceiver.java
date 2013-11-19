/*
 * Copyright (c) 2013, University of Hohenheim Department of Informations Systems 2
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>
 */
package zeroMQ.receiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;
import org.first.messaging.Messenger;

import utils.GateInitSingleton;
import utils.GlobalParameters;
import zeroMQ.messageQueue.CompletePipelineWorker;
import zeroMQ.messageQueue.MessageFileQueue;
import zeroMQ.messageQueue.WorkerThread;

/**
 * @author lgredel
 *  
 *  Main Class, which receives a message from ZeroMQ
 *  and put these message to MessageFileQueue Instance
 *
 */
public class CompletePipelineReceiver {

  public static Properties configProperties = GlobalParameters.loadConfigFile();

  public static Logger log = Logger.getLogger(CompletePipelineReceiver.class.getName());

  private static MessageFileQueue queue = null;

  public static Messenger messenger = null;

  public static ArrayList<Thread> currenThreadList = CompletePipelineWorker.getCurrenThreadList();

  private static int maxMessageBufferSize = GlobalParameters.extractMaxMessageBufferSize();

  private static final int SLEEP_TIMEOUT = GlobalParameters.extractSleepTimeout();
  
  /**
   * @param args
   */
  public static void main(String[] args) {

    extractPID();
    
    try {
      queue = MessageFileQueue.getInstance();
    } catch (FileNotFoundException e2) {
      log.error("Cannot instanciate MessageFileQueue");
      log.error("System exit manually");
      System.exit(-987);
    }

    String maxMessageLengthStr = configProperties.get("maxMessageLength").toString();
    int maxMessageLength = 0;
    try {
      maxMessageLength = new Integer(maxMessageLengthStr).intValue();
    } catch (NumberFormatException nfe) {
      log.error("Cannot read MaxStringLength of document " + maxMessageLengthStr
          + "as int-Value from config-File");
      log.error(nfe.getMessage());
      maxMessageLength = 1000000;
      log.info("Setting maxMessageLength to default: " + maxMessageLength);
    }
    // Creates messaging thread
    messenger = new Messenger();

    try {
      CompletePipelineWorker.inizializeWorker(messenger);
    } catch (Exception e1) {
      log.error(e1.getClass() + "occured on initializing PreprocessingWorker");
      log.error(e1.getMessage());
      log.error("System exit");
      System.exit(-77);
    }

    long messageNum = 0;
    int tries = 0;

    boolean loop = true;
    Random rand = new Random();

    String message = null;
    
    while (loop) {

      int queueSize = queue.getFileMessages().size();
      
      generateRandomCommentOfMessageQueueSize(rand, queueSize);

      int messageLength = 0;
      
      if(message == null){
                
        
        /**
         * Receives a message from ZeroMQ
         */
        message =  messenger.getMessage();
               
      }

      
      if (message != null ) {
      
        /**
         * Message Received
         */
        messageLength = message.length();
                
        if (messageLength > maxMessageLength) {
          
          log.info("Documentsize to big: " + messageLength);
          message = null;
          continue;
        } 
        
        if (message.equals(Messenger.FINISH_COMMAND)) {
          
          receivedFinishCommand();

          log.info("SEND FINISH COMMAND TO NEXT 0MQ-STEP");
          messenger.sendMessage(message);
          break;
        } else {

          
          boolean fileOffered =  queue.pushMessage(message);        

          if(fileOffered){
            ++messageNum;
            log.info("Receiver added message: " + messageNum + " with messageSize: " + messageLength);
                
            message = null;
            tries = 0;   
                        
          }else{
           
            try {
              Thread.sleep(SLEEP_TIMEOUT);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            tries++;
                     
            if ((tries % 50) == 0) {
              log.info("Queue is full");
              System.out.println("Tried " + tries + " to send same document");
            }
          }
        }
      }
    }

    /**
     * Wait for finish on all Threads on queue
     */
    Iterator threadIt = currenThreadList.iterator();

    while (threadIt.hasNext()) {

      Thread currentThread = (Thread) threadIt.next();
      try {
        currentThread.join();

      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    GateInitSingleton.unloadGateResources();
    log.info("Total received messages:" + messageNum);
  }


  /**
   * Generates an minimized random console-output of current documents in queue
   * 
   * @param rand
   * @param queueSize
   */
  private static void generateRandomCommentOfMessageQueueSize(Random rand, int queueSize) {
    int n = 1000000;
    int tmp = rand.nextInt(n);
    tmp++;

    int mod = (n % tmp);
    
    if (mod == 0) {
      System.out.println("Actual number of documents in queue is: " + queueSize
          + " from: " + maxMessageBufferSize);
    }
  }

  
  /**
   * Extract the ProcessID from current Java-Process and write this into PID.txt 
   */
  private static void extractPID() {
    
    PrintWriter printWriter = null;
    try {
        java.lang.management.RuntimeMXBean runtime = java.lang.management.ManagementFactory.getRuntimeMXBean();
        java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
        jvm.setAccessible(true);
        sun.management.VMManagement mgmt =  (sun.management.VMManagement) jvm.get(runtime);
        java.lang.reflect.Method pid_method =  mgmt.getClass().getDeclaredMethod("getProcessId");
        pid_method.setAccessible(true);
 
        int pid = (Integer) pid_method.invoke(mgmt);
        log.info("Extracted PID");
        
        File pidFile = new File ("PID.txt");        
        printWriter = new PrintWriter (pidFile);
        printWriter.println (pid);
      
    } catch (FileNotFoundException e) {
      log.error("Error writing PID FILE");
      log.error(e.getMessage());
    } catch (SecurityException e) {
      log.error("Exception " + e.getClass().getName() + " occured on extract ProcessID (PID) from VM");
      log.error(e.getMessage());
    } catch (NoSuchFieldException e) {
      log.error("Exception " + e.getClass().getName() + " occured on extract ProcessID (PID) from VM");
      log.error(e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("Exception " + e.getClass().getName() + " occured on extract ProcessID (PID) from VM");
      log.error(e.getMessage());
    } catch (IllegalAccessException e) {
      log.error("Exception " + e.getClass().getName() + " occured on extract ProcessID (PID) from VM");
      log.error(e.getMessage());
    } catch (NoSuchMethodException e) {
      log.error("Exception " + e.getClass().getName() + " occured on extract ProcessID (PID) from VM");
      log.error(e.getMessage());
    } catch (InvocationTargetException e) {
      log.error("Exception " + e.getClass().getName() + " occured on extract ProcessID (PID) from VM");
      log.error(e.getMessage());
    }finally{
      if(printWriter != null){
        printWriter.close ();
      }
    }   
  }

  
  /**
   * If FINISH-Command is received from ZeroMQ, stop/interrupt all Workerthreads
   * on empty-MessageQueue
   */
  public static void receivedFinishCommand() {
    log.info("Received finish command");

    Iterator<Thread> stopThreadsIt = currenThreadList.iterator();

    while (stopThreadsIt.hasNext()) {
      WorkerThread th = (WorkerThread) stopThreadsIt.next();
      log.trace("Starting to interrupt thread: " + th.getName());

      Integer waittime = new Integer(configProperties.get("gracefulTimeout").toString());

      while (true) {
        log.info("Waiting to stop Thread ... " + th.getName());
        try {
          Thread.sleep(waittime);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

        if (queue.getFileMessages().peek() == null) {
          log.info("Interrupt Thread ..." + th.getName());
          th.interrupt();
          break;
        } else {
          System.out.println("Waiting: ...");
        }
      }
    }
  }
}
