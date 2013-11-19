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
package classification;

import hibernate.DatabaseFacade;
import hibernate.entities.DocumentMetaData;
import hibernate.entities.SentimentFeatureType;
import hibernate.entities.SentimentObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;

import utils.GlobalParameters;


/**
 * Class used for serialization and storage of the processed document 
 * including some helper methods.
 * 
 * @author lgredel, oaltuntas
 *
 */
@XmlType(propOrder = { "author", "url", "publicationDate", "title", "documentText", "sentimentDocuments" })
@XmlRootElement(name = "Document")
public class ClassifiedDocument {
  @XmlTransient
  private static Logger log = Logger.getLogger(ClassifiedDocument.class);
  @XmlTransient
  private static Properties configProperties = GlobalParameters.loadConfigFile();

  private String author;
  private String url;
  private String title;
  private String publicationDate;

  private String documentText;

  //DatabaseData
  @XmlTransient
  DocumentMetaData docMeta = null;

  /**
   * ArrayList containing all Sentiments on document level (SentimentDocument)
   * belonging to the current document.
   */
  private ArrayList<SentimentDocument> sentimentDocuments = new ArrayList<SentimentDocument>();

  
  /**
   * Delete old Sentiments in database for the current DocumentMetaData object. 
   * 
   * @param dbDocument - Database-Object for the current document
   * @throws Exception
   */
  public ClassifiedDocument(DocumentMetaData dbDocument) throws Exception {

    this.docMeta = dbDocument;

      DatabaseFacade.getSingletonFacade().deleteSentimentsWithClassifierType(docMeta,
        "Knowledge-Based", "CRISP");
        
    boolean fuzzyClassification = Boolean.parseBoolean(configProperties.get("executeHybridFuzzyClassifier").toString());
    
    if(fuzzyClassification){
      DatabaseFacade.getSingletonFacade().deleteSentimentsWithClassifierType(docMeta, "Hybrid KnowledgeBased MachineLearning", "FUZZY");
    } 

    log.info("Create new ClassifiedDocument without DocumentMetaData-Object");

    Date retrievalDate = new Date();
    docMeta.setRetrievalDate(retrievalDate);
  }

  /**
   * extract if an sentimentDocument per object and feature exists and returns
   * this SentimentDocument
   */
  public SentimentDocument existsSentimentDocument(SentimentObject sentObject,
      SentimentFeatureType SentFeature) {

    SentimentDocument currentDocLevel = null;

    ArrayList<SentimentDocument> extractedSentimentDocuments = sentimentDocuments;

    for (int i = 0; i < extractedSentimentDocuments.size(); i++) {

      SentimentDocument sentimentDocument = extractedSentimentDocuments.get(i);

      String docSentFeature = sentimentDocument.getSentimentFeature().getName();
      String docSentObj = sentimentDocument.getSentimentObject().getName();

      if ((docSentFeature.equalsIgnoreCase(SentFeature.getName()))
          && (docSentObj.equalsIgnoreCase(sentObject.getName()))) {

        currentDocLevel = sentimentDocument;
        log.debug("Current Documentlevel alread exists as sentiment in database with id:"
            + sentimentDocument.getDbsentiment().getId());
        break;
      }
    }
    return currentDocLevel;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDocumentText() {
    return documentText;
  }

  public void setDocumentText(String documentText) {
    this.documentText = documentText;
  }

  @XmlElement(name = "sentimentDocument")
  public ArrayList<SentimentDocument> getSentimentDocuments() {
    return sentimentDocuments;
  }

  public void setSentimentDocuments(ArrayList<SentimentDocument> sentimentDocuments) {
    this.sentimentDocuments = sentimentDocuments;
  }

  public String getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(String artDate) {
    this.publicationDate = artDate;
  }

  @XmlTransient
  public DocumentMetaData getDocMeta() {
    return docMeta;
  }

  public void setDocMeta(DocumentMetaData docMeta) {
    this.docMeta = docMeta;
  }
}
