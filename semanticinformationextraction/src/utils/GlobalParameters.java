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
package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

public class GlobalParameters {
  private static Logger log = Logger.getLogger(GlobalParameters.class);
  private static File log4File = new File("resources" + File.separator + "config", "log4j.properties");
  private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
  private static SimpleDateFormat pubDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private static final String CONFIG_FILENAME = "resources" + File.separator + "config" + File.separator
      + "pipeline_config.xml";

  private static File applicationDir = new File("resources" + File.separator + "JSI+UHOH"
      + File.separator + "application");

  private static File uhohClassificationGateApp = new File(applicationDir, "Classification_UHOH.gapp");
  private static File jsiClassificatonGateApp = new File(applicationDir, "Classification_JSI.gapp");
  private static File jsiPreprocessingGateApp = new File(applicationDir, "Preprocessing_JSI.gapp");
  private static File uhohPreprocessingGateApp = new File(applicationDir, "Preprocessing_UHOH.gapp");
  private static File preperationSOApp = new File(applicationDir, "preparation_SO.gapp");
  private static File docResetApp = new File(applicationDir, "DocumentResetApplication.gapp");

  private static File machineLearningDir = new File("resources" + File.separator + "MachineLearning" + File.separator
      + "HybridFuzzy");
  
  /*Hybrid - Fuzzy GateApps*/
  private static File classify_apply_gapp_Pos =  new File(applicationDir,"HybridFuzzy"+ File.separator + "application_pos.gapp");
  private static File classify_apply_gapp_Neg =  new File(applicationDir,"HybridFuzzy"+ File.separator + "application_neg.gapp");
  private static File hybridGateApp = new File(applicationDir,"Hybrid_Classification.gapp");
  
  private static final File ontologyFile = new File("resources" + File.separator + "JSI+UHOH"
      + File.separator + "ontology" + File.separator + "FIRSTOntology.owl");
  
  private static Properties configProperties = null;

  // 256 kB stopping Gateprocessing
  private static final long FREE_MEMORY_SIZE = 256 * 1024;
  // Timout for the executiontime of gateapplication
  private static final long APPLICATION_WAITTIME = 5 * 60 * 1000;
  // Timeintervall for Thread,
  private static final long SLEEPTIME = 50;
  
  //size of queuebuffer
  private static int maxMessageBufferSize = 100;
  private static int ThreadSleepTime;
  private static Integer TRANSACTION_TIMEOUT = null;

  public synchronized static Properties loadConfigFile() {
    if (configProperties == null) {
      configProperties = new Properties();
      FileInputStream fileInputStream = null;
      try {
        String configFilePath = System.getProperty("user.dir") + File.separator + CONFIG_FILENAME;
        File file = new File(configFilePath);

        fileInputStream = new FileInputStream(file);
        configProperties.loadFromXML(fileInputStream);
      } catch (FileNotFoundException e) {
        // JOptionPane.showMessageDialog(null,bundle.getString("error.xmlFileNotExists")
        // +"\n" + CONFIG_FILENAME);
        log.error(e.getMessage());
        System.exit(JFrame.DISPOSE_ON_CLOSE);
      } catch (IOException e) {
        // JOptionPane.showMessageDialog(null,bundle.getString("error.xmlFileNotExists")
        // +"\n" + CONFIG_FILENAME);
        log.error(e.getMessage());
        System.exit(JFrame.DISPOSE_ON_CLOSE);
      } finally {
        try {
          fileInputStream.close();
          fileInputStream = null;
          System.gc();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    return configProperties;
  }

  public static String getConfigFilename() {
    return CONFIG_FILENAME;
  }

  public static File getLog4File() {
    return log4File;
  }

  public static void setLog4File(File log4File) {
    GlobalParameters.log4File = log4File;
  }

  public static SimpleDateFormat getFormat() {
    return format;
  }

  public static void setFormat(SimpleDateFormat format) {
    GlobalParameters.format = format;
  }

  /**
   * @param currentFeatureName
   * @return
   */
  public static String changeFeatureName(String currentFeatureName) {
    // TODO: Evtl. in Ontology ändern ???
    if (currentFeatureName.equalsIgnoreCase("Reputation")) {
      currentFeatureName = "expectedFutureReputationChange";
    }
    if (currentFeatureName.equalsIgnoreCase("Price")) {
      currentFeatureName = "expectedFuturePriceChange";
    }
    if (currentFeatureName.equalsIgnoreCase("Volatility")) {
      currentFeatureName = "expectedFutureVolatilityChange";
    }
    return currentFeatureName;
  }

  public static long getFreeMemorySize() {

    return FREE_MEMORY_SIZE;
  }

  public static long getApplicationWaittime() {
    return APPLICATION_WAITTIME;
  }

  public static long getSleepTime() {

    return SLEEPTIME;
  }

  public static File getShareDirectory() throws FileNotFoundException {

    if (configProperties == null) {
      configProperties = loadConfigFile();
    }

    String hostIP = configProperties.getProperty("serverIP");
    String shareName = configProperties.getProperty("shareName");
    File shareDir = null;

    try {
      if (SystemUtils.IS_OS_WINDOWS) {

        shareDir = new File(File.separator + File.separator + hostIP + File.separator + shareName);
        log.info("Setting ShareDirectory on Windows OS to file: " + shareDir.getAbsolutePath());

      } else {
        if (SystemUtils.IS_OS_LINUX) {
          shareDir = new File(shareName);
          log.info("Setting mountet ShareDirectory on Linux OS to file: " + shareDir.getAbsolutePath());
        } else {
          log.error("No Share-Directory from Properties file with serverIP: " + hostIP
              + " and shareName: " + shareName + " detected.");
          throw new FileNotFoundException("SystemUtils cannot determine OS");
        }
      }
    } catch (NullPointerException nullPointer) {
      throw new FileNotFoundException("Cannot instanciare shareDir-File Object from configProperties:" +
      		"\nhostIP:\t" + hostIP + "\tshareName:\t" + shareName );
    }
    return shareDir;
  }

  public static String extractCurrentPreprocessingDocumentType() {
    String executionPipeline = configProperties.getProperty("pipelineExecution");

    String fileType = "preprocessed";

    if (executionPipeline.equalsIgnoreCase("JSI")) {
      fileType = "JSI-preprocessed";
    } else {
      if (executionPipeline.equalsIgnoreCase("UHOH")) {
        fileType = "preprocessed";
      }
    }
    return fileType;
  }

  public static File zipStringToFile(String currentMessageString, File outputZipFile) throws IOException {

    log.trace("Starting to zip messageString into File");

    ZipOutputStream outStream = null;
    String zipEntryName = outputZipFile.getName();

    outputZipFile = new File(outputZipFile.getParent(), outputZipFile.getName() + ".zip");

    try {
      outStream = new ZipOutputStream(new FileOutputStream(outputZipFile));

      // Add a zip entry to the output stream
      outStream.putNextEntry(new ZipEntry(zipEntryName));
      outStream.write(currentMessageString.getBytes("utf-8"));

      // Close zip entry and file streams

      outStream.closeEntry();
      outStream.close();
      outStream = null;
      System.gc();
    } catch (IOException e) {
      log.error("IOException on writing String to zipFile: " + outputZipFile.getAbsolutePath());
      throw e;
    }

    return outputZipFile;
  }

  public static void zipFile(File file) {
    try {

      String zipFileName = file.getName() + ".zip";
      File outputFile = new File(file.getParentFile(),zipFileName);
      
      // Create input and output streams
      FileInputStream inStream = new FileInputStream(file);
      ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(outputFile));

      // Add a zip entry to the output stream
      outStream.putNextEntry(new ZipEntry(file.getName()));

      byte[] buffer = new byte[1024];
      int bytesRead;

      // Each chunk of data read from the input stream
      // is written to the output stream
      while ((bytesRead = inStream.read(buffer)) > 0) {
        outStream.write(buffer, 0, bytesRead);
      }

      // Close zip entry and file streams
      outStream.closeEntry();

      outStream.close();
      inStream.close();

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public static File unzipFile(File txtZipFile) throws IOException {

    ZipFile zipFile = null;
    File unzipTxtFile = null;
    try {
      zipFile = new ZipFile(txtZipFile);
      Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();

      while (entries.hasMoreElements()) {

        ZipEntry entry = (ZipEntry) entries.nextElement();

        InputStream in = zipFile.getInputStream(entry);
        OutputStream out = new FileOutputStream(entry.getName());

        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0)
          out.write(buffer, 0, len);

        in.close();
        out.close();

        unzipTxtFile = new File(txtZipFile.getParentFile(), entry.getName());
        log.info("Unzipped txtFile: " + txtZipFile.getAbsolutePath() + " to "
            + unzipTxtFile.getAbsolutePath());
      }

      zipFile.close();
    } catch (ZipException e) {
      log.error("ZipException on unzip txtFile: " + txtZipFile.getAbsolutePath() + " occured");
      throw e;
    } catch (IOException e) {
      log.error("IOException on unzip txtFile " + txtZipFile.getAbsolutePath() + " occured");
      throw e;
    }

    return unzipTxtFile;
  }

  public static void deleteFile(File fileToDelete) {
    fileToDelete.setWritable(true);
    try {
      fileToDelete = fileToDelete.getCanonicalFile();
    } catch (IOException e) {
      log.error("IP-Exception on CanonicalFile: " + fileToDelete.getAbsolutePath());
    }

    if (!fileToDelete.canWrite()) {
      log.trace("Set writable flag for file to delete");
      fileToDelete.setWritable(true);
    }

    boolean deleteFile = fileToDelete.delete();
    if (deleteFile) {
      log.info("Delete file: " + fileToDelete.getAbsolutePath() + " sucessfully.");
    } else {
      log.error("Cannot delete file: " + fileToDelete.getAbsolutePath());
      log.info("Try delete on exit.");
      fileToDelete.deleteOnExit();
    }
  }
  
  /**
   * Folderstrucutre to store Files should be: SharedDirectory\Year\Mont\Day
   * if necesarry creates the folder
   * 
   * @param publicationDate
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static File createTmpDirectoriesFromDate(Date publicationDate) throws FileNotFoundException, IOException {
    log.trace("createDirectoryFromDate: " + publicationDate);
    String tempFolder = System.getProperty("java.io.tmpdir");  
    File outputDir = new File(tempFolder);           
    
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(publicationDate);
    
    Integer year = calendar.get(Calendar.YEAR);
    //JANUARY = 0
    Integer month = calendar.get(Calendar.MONTH);
    month++;
    Integer day = calendar.get(Calendar.DAY_OF_MONTH);
    
    outputDir = new File(outputDir,year.toString() + File.separator + month.toString() + File.separator + day.toString());
    
    if((outputDir != null) && (!outputDir.exists())){
      boolean dirCreated = outputDir.mkdirs();
      if(dirCreated){
        log.trace("Created directory: " + outputDir.getAbsolutePath());
      }else{
        String errorMessage = "Error creating directory from publicationDate: " + GlobalParameters.getFormat().format(publicationDate) + " and sharedDirectory: " + GlobalParameters.getShareDirectory().getAbsolutePath(); 
        log.error(errorMessage);
        throw new IOException(errorMessage);
      }
    }
    return outputDir;
  }
  
  /**
   * @return 
   * 
   */
  public static int extractMaxMessageBufferSize() {
    String maxMessageBufferSizeStr = configProperties.get("maxMessageBufferSize").toString();
    
    try{
      maxMessageBufferSize = new Integer(maxMessageBufferSizeStr).intValue();
    }catch (NumberFormatException nfe) {
      log.error("Cannot read MaxQueueSize "+ maxMessageBufferSizeStr +"as int-Value from config-File");
      log.error(nfe.getMessage());
      maxMessageBufferSize = 100;
      log.info("Setting MaxMessageBufferSize to default: " + maxMessageBufferSize);      
    }
    
    return maxMessageBufferSize;
  }
  
  public static int extractSleepTimeout() {
    
    String maxTimeOutStr = configProperties.get("ThreadSleepTime").toString();
    
    try{
      ThreadSleepTime = new Integer(maxTimeOutStr).intValue();
    }catch (NumberFormatException nfe) {
      log.error("Cannot read ThreadSleepTime "+ maxTimeOutStr +"as int-Value from config-File");
      log.error(nfe.getMessage());
      ThreadSleepTime = 1000;
      log.info("Setting ThreadSleepTime to default: " + ThreadSleepTime);      
    }
    
    return ThreadSleepTime;
  }
  
  /*
   * Timeout in seconds for a database Transaction
   * if there`s a problem reading it from config file default value is set to 2 Min. sec
   * */
  private static int extractTransactionTimeout() {
    String transactionTimeout = configProperties.get("TransactionTimeout").toString();
    
    try{
      TRANSACTION_TIMEOUT = new Integer(transactionTimeout).intValue();
    }catch (NumberFormatException nfe) {
      log.error("Cannot read TransactionTimeout "+ TRANSACTION_TIMEOUT +"as int-Value from config-File");
      log.error(nfe.getMessage());
      TRANSACTION_TIMEOUT = 120;
      log.info("Setting MaxMessageBufferSize to default: " + maxMessageBufferSize);      
    }
    
    return TRANSACTION_TIMEOUT;
  }

  /*
   * Timeout in seconds for a database Transaction
   * */
  public static Integer getTRANSACTION_TIMEOUT() {
    
    if( TRANSACTION_TIMEOUT == null){
      TRANSACTION_TIMEOUT = extractTransactionTimeout();
    }
    
    return TRANSACTION_TIMEOUT;
  }

  public static File getJsiPreprocessingGateApp() {
    return jsiPreprocessingGateApp;
  }

  public static void setJsiPreprocessingGateApp(File jsiPreprocessingGateApp) {
    GlobalParameters.jsiPreprocessingGateApp = jsiPreprocessingGateApp;
  }

  public static File getUhohPreprocessingGateApp() {
    return uhohPreprocessingGateApp;
  }

  public static File getPreperationSOApp() {
    return preperationSOApp;
  }

  public static File getDocResetApp() {
    return docResetApp;
  }

  public static File getOntologyFile() {

    return ontologyFile;
  }

  public static File getJsiClassificatonGateApp() {
    return jsiClassificatonGateApp;
  }

  public static void setJsiClassificatonGateApp(File jsiClassificatonGateApp) {
    GlobalParameters.jsiClassificatonGateApp = jsiClassificatonGateApp;
  }

  public static File getUhohClassificationGateApp() {
    return uhohClassificationGateApp;
  }

  public static void setUhohClassificationGateApp(File uhohClassificationGateApp) {
    GlobalParameters.uhohClassificationGateApp = uhohClassificationGateApp;
  }

  public static SimpleDateFormat getPubDateFormat() {
    return pubDateFormat;
  }
  
  public static File getClassify_apply_gapp_Pos() {
    return classify_apply_gapp_Pos;
  }

  public static File getClassify_apply_gapp_Neg() {
    return classify_apply_gapp_Neg;
  }

  public static File getHybridGateApp() {
    return hybridGateApp;
  }
  
  public static File getMachineLearningDir() {
    return machineLearningDir;
  }
}
