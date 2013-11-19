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
package zeroMQ.messageQueue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import utils.GlobalParameters;

/**
 * 
 * Singleton Instance Class, which contains a LinkedBlockingQueue
 * This queue contains Gate-XML-Files to process. 
 * 
 * @author lgredel
 *
 */
public class MessageFileQueue {
  private static Logger log = Logger.getLogger(MessageFileQueue.class);
  private LinkedBlockingQueue<String> fileMessages = null;
  private static Properties configProperties = GlobalParameters.loadConfigFile();
  private static File receivedFileDir = null;

  private static MessageFileQueue messageFileQueue = null;

  private MessageFileQueue() throws FileNotFoundException {
    super();
    
    int capacity = GlobalParameters.extractMaxMessageBufferSize();
    fileMessages = new LinkedBlockingQueue<String>(capacity);
    
    try{
    receivedFileDir = GlobalParameters.getShareDirectory();
    }catch(FileNotFoundException fnfex){
      log.error(fnfex.getClass().getName() + " occured on initializing receivedFileDir from ShareDirectory");
      log.error(fnfex.getMessage());
      throw fnfex;
    }
    
    if (!receivedFileDir.isDirectory()) {
      receivedFileDir = new File(configProperties.get("receivedFilesDirectory").toString());
    }
    receivedFileDir.setWritable(true);
  }

  
  /**
   * @return Singleton-Instance of MessageFileQueue
   * 
   * @throws FileNotFoundException
   */
  public static synchronized MessageFileQueue getInstance() throws FileNotFoundException {

    if (messageFileQueue == null) {
      log.info("Creating new MessageFileQueue");
        messageFileQueue = new MessageFileQueue();
    }

    return messageFileQueue;
  }

  public LinkedBlockingQueue<String> getFileMessages() {
    return fileMessages;
  }

  public void setFileMessages(LinkedBlockingQueue<String> fileMessages) {
    this.fileMessages = fileMessages;
  }


  /**
   * Retrieves/removes the head-Element from queue with take
   * 
   * @return the Element containing the gate-xml message to process
   * @throws InterruptedException
   */
  public String popMessage() throws InterruptedException{

    String currentStringMessage = null;
    try {
      
      log.debug("Preparing to take");
       currentStringMessage = fileMessages.take();
          
      log.debug("Message taken from Queue");
    } catch (InterruptedException e) {
      log.error(e.getClass() + " occured on taken a file from queue");    
    }

    return currentStringMessage;
  }

  /**
   * Insert the currentMessage in the queue (tail-position)
   * 
   * @param currentMessageString the message to add
   * @return true if the element was added to this queue, else false
   * 
   * @throws NullPointerException
   */
  public boolean pushMessage(String currentMessageString) throws NullPointerException{
    boolean fileOfferd = false;
    try{
        fileOfferd = fileMessages.offer(currentMessageString);
    }catch(NullPointerException npe){
      log.error("NullpointerException on offer StringMessage to queue occured");
      throw npe;
    }
    
    return fileOfferd;
  }
}
