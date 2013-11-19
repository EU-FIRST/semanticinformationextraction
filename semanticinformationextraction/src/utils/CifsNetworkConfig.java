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
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

/**
 * 
 * Singleton Class which contains the configuration of a connection to a remote
 * host and a network-share where to send and save result-files.
 * 
 * Sending files through network using CIFS/SMB-protocol from JCIFS Client
 * library
 * 
 * @author lgredel
 * 
 */
public class CifsNetworkConfig {

  private static Logger log = Logger.getLogger(CifsNetworkConfig.class);
  private static Properties configProperties = GlobalParameters.loadConfigFile();

  private final String SMB_PREFIX = "smb:";
  private static String USERNAME = null;
  private static String PASSWORD = null;
  private String userInfo = null;
  private String serverIP = null;
  private String shareName = null;

  private String NETWORK_FOLDER = null;

  private NtlmPasswordAuthentication auth = null;

  private SmbFile currentSmbFile = null;

  public static CifsNetworkConfig INSTANCE = null;

  /**
   * Constructor with initializing connection parameters from config-File
   */
  private CifsNetworkConfig() {
    super();

    USERNAME = configProperties.get("user").toString();
    PASSWORD = configProperties.get("password").toString();
    serverIP = configProperties.get("serverIP").toString();
    shareName = configProperties.get("shareName").toString();

    NETWORK_FOLDER = SMB_PREFIX + "//" + serverIP + shareName;
    String user = USERNAME + ":" + PASSWORD;

    auth = new NtlmPasswordAuthentication(user);

    log.debug("Set Network Authentication");
    log.debug("server:" + serverIP + "\tshareName: " + shareName + "\tuser: " + USERNAME);
  }

  /**
   * Return the singleton-instance for Network configuration, 
   * or create a new one if it`s not initialized
   * 
   * @return
   */
  public synchronized static CifsNetworkConfig getINSTANCE() {

    if (INSTANCE == null) {
      INSTANCE = new CifsNetworkConfig();
    }

    return INSTANCE;
  }

  /**
   * Copy a file to the network-share directory, if the parentdirectorys not on
   * the windows share these directorys will be created If sending is
   * sucessfully, input source file will be deleted
   * 
   * @param deleteSourceFile
   * @param sourceFile
   *          : File with the subfoder-directories to store on the network share
   * 
   * @return the Complete windows network path where the sourcefile is copied
   */
  public SmbFile copyFile(File sourceFile, boolean deleteSourceFile) throws IOException {

    String tempFolder = System.getProperty("java.io.tmpdir");
    String relativeDestFilePath = sourceFile.getAbsolutePath();
    relativeDestFilePath = relativeDestFilePath.substring(relativeDestFilePath.indexOf(tempFolder)
        + tempFolder.length());

    if (SystemUtils.IS_OS_WINDOWS) {
      relativeDestFilePath = relativeDestFilePath.replace(File.separatorChar, '/');
    }

    String destinationFilePath = NETWORK_FOLDER + relativeDestFilePath;

    networkCopy(sourceFile, destinationFilePath, deleteSourceFile);
    return currentSmbFile;
  }

  /**
   * Copy an Ontology-File to the 'Ontology' folder on the Network Share.
   * Backup and rename the ontology-file with the current Date as suffix.
   * 
   * @param sourceOntologyFile current Ontology file to send
   * @param deleteSourceFile delete local Ontology file after sending it successfully to the network share
   * @return Remote network path where the file was saved
   * 
   * @throws Exception
   */
  public String copyOntologyFile(File sourceOntologyFile, boolean deleteSourceFile) throws Exception {

    String path = NETWORK_FOLDER + "Ontology";

    String destinationOntologyVersionFile = path + "/" + sourceOntologyFile.getName() + "_"
        + GlobalParameters.getFormat().format(new Date());

    networkCopy(sourceOntologyFile, destinationOntologyVersionFile, deleteSourceFile);

    return destinationOntologyVersionFile;
  }

  /**
   * Copy file per JCIFS Client library using CIFS/SMB-protocol
   * 
   * @param sourceFile File to send
   * @param destinationFilePath destination path where to send the file
   * @param deleteSourceFile delete source file after it has been successfully sent to the network share
   * 
   * @throws IOException
   */
  public void networkCopy(File sourceFile, String destinationFilePath, boolean deleteSourceFile)
      throws IOException {
    SmbFileOutputStream sfos = null;
    FileInputStream sourceInputStream = null;

    try {
      this.currentSmbFile = new SmbFile(destinationFilePath, auth);

      if (!currentSmbFile.exists()) {
        log.debug("SMB file will be created new: " + currentSmbFile.getPath());
        String parentDirPath = currentSmbFile.toString();
        parentDirPath = parentDirPath.substring(0, parentDirPath.lastIndexOf('/') + 1);

        SmbFile parentDir = new SmbFile(parentDirPath, auth);
        if (!parentDir.exists()) {
          log.debug("SMB directory for smb-file will be created new: " + parentDir.getPath());
          parentDir.mkdirs();
        }

        currentSmbFile.createNewFile();
      }

      sfos = new SmbFileOutputStream(currentSmbFile);

      sourceInputStream = new FileInputStream(sourceFile);
      IOUtils.copy(sourceInputStream, currentSmbFile.getOutputStream());

      log.info("Send file: " + sourceFile.getAbsolutePath() + " sucessfully to "
          + currentSmbFile.getPath());

    } catch (IOException e) {
      log.error(e.getClass().getName() + " occured on sending file: " + sourceFile.getAbsolutePath());
      log.error(e.getMessage());
      throw e;
    } finally {
      if (sfos != null) {
        try {
          sfos.close();
        } catch (IOException e) {
          log.error("Cannot close smbFileOutputstream");
          log.error(e.getMessage());
          throw e;
        }
      }

      if (sourceInputStream != null) {
        sourceInputStream.close();
      }

      if (deleteSourceFile) {
        GlobalParameters.deleteFile(sourceFile);
      }
    }
  }

  public String getServerIP() {
    return serverIP;
  }

  public void setServerIP(String serverIP) {
    this.serverIP = serverIP;
  }

  public String getShareName() {
    return shareName;
  }

  public void setShareName(String shareName) {
    this.shareName = shareName;
  }

  public String getSmbPrefix() {
    return SMB_PREFIX;
  }

  public SmbFile getCurrentSmbFile() {
    return currentSmbFile;
  }

  public void setCurrentSmbFile(SmbFile currentSmbFile) {
    this.currentSmbFile = currentSmbFile;
  }

  public String getNETWORK_FOLDER() {
    return NETWORK_FOLDER;
  }
}
