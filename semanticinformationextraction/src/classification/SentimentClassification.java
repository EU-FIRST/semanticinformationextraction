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

import fuzzyClassification.NewApproachHybridFuzzy;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.DocumentContent;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import hibernate.HibernateConstantEntities;
import hibernate.DatabaseFacade;
import hibernate.entities.DocumentMetaData;
import hibernate.entities.Indicator;
import hibernate.entities.IndicatorType;
import hibernate.entities.Phrase;
import hibernate.entities.PhraseType;
import hibernate.entities.Sentiment;
import hibernate.entities.SentimentClassifierType;
import hibernate.entities.SentimentFeatureType;
import hibernate.entities.SentimentIndicator;
import hibernate.entities.SentimentLevelDefinition;
import hibernate.entities.SentimentObject;
import hibernate.entities.SentimentObjectType;
import hibernate.entities.SentimentPhraseRelation;

import java.io.File;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;

import utils.GateInitSingleton;
import utils.GlobalParameters;
import utils.UHOH_GateApplication;

/**
 * TODO: purpose of the class
 * 
 * @author Lilli Gredel
 * 
 */
public class SentimentClassification extends UHOH_GateApplication {

  private ClassifiedDocument classifiedDocument;

  private Boolean EXECUTE_FUZZY = null;

  private DatabaseFacade databaseConn = DatabaseFacade.getSingletonFacade();

  public SentimentClassification(CorpusController gateApp) {
    super();
    application = gateApp;
    EXECUTE_FUZZY = Boolean
        .parseBoolean(configProperties.get("executeHybridFuzzyClassifier").toString());
    log.info("Setting EXECUTE_FUZZY to: " + EXECUTE_FUZZY);
  }

  /**
   * @param inputFileName
   * @param dbDocument
   * @throws Exception
   * 
   * 
   */
  @SuppressWarnings("unchecked")
  public Document executeDocument(Document gateDoc, DocumentMetaData dbDocument) throws Exception {

    log.debug("Starting classifying document from: " + gateDoc.getName());

    long appRunStart = 0;
    long appRunEnd = 0;

    Corpus corpus = null;
    try {
      corpus = Factory.newCorpus(gateDoc.getName() + "_Corpus");

    } catch (ResourceInstantiationException e) {
      log.error("ResourceInstantiationException on creating Corpus on document: " + gateDoc.getName()
          + " and databaseID: " + dbDocument.getId());
      GateInitSingleton.executeResetApplication(gateDoc);
      GateInitSingleton.unloadGateResources(gateDoc, corpus);
      throw e;
    }

    // Extract InputSentimentObjects aus preprocessed Input-XML
    AnnotationSet preprocessedAnnotations = gateDoc.getAnnotations();

    Set<String> preAnnotType = preprocessedAnnotations.getAllTypes();

    corpus.add(gateDoc);

    // Applikation auf Corpus ausführen
    application.setCorpus(corpus);

    try {

      appRunStart = new Date().getTime();

      application.execute();

      log.info("Application :'" + application.getName() + "' executet on Corpus: '" + corpus.getName()
          + "'");
      appRunEnd = new Date().getTime();
      long classificationRunTime = appRunEnd - appRunStart;
      log.info("--------------------------------------------------------");
      log.info("Application run time: " + classificationRunTime);
      log.info("on Gatedocument: " + gateDoc.getName());
      log.info("with inputXML: '" + gateDoc.getName());
      log.info("--------------------------------------------------------");

      timeMeasurement.setClassificationRunTime(classificationRunTime);

      /*
       * If preprocessed inputXML fileName is in database, documentMetaData is
       * set, else create new DocumentMetaData object
       */
      classifiedDocument = new ClassifiedDocument(dbDocument);

      AnnotationSet annotations = gateDoc.getAnnotations("Sentiment");
      // Sentiment aggregation process
      this.defineSentimentDocuments(gateDoc, annotations, dbDocument);

    } catch (ExecutionException e) {
      log.error("Cannot Execute Application on Corpus: '" + corpus.getName() + "'");
      log.error(e.getMessage());

      GateInitSingleton.executeResetApplication(gateDoc);
      GateInitSingleton.unloadGateResources(gateDoc, corpus);
      throw e;
    } catch (Exception ex) {
      GateInitSingleton.executeResetApplication(gateDoc);
      GateInitSingleton.unloadGateResources(gateDoc, corpus);
      throw ex;
    }

    GateInitSingleton.unloadGateResources(null, corpus);

    return gateDoc;
  }

  /**
   * calculate the sentiment of a Gate-Document on document-Level
   * 
   * @param sentimentAnnotationSet
   * @throws Exception 
   */
  private void defineSentimentDocuments(Document doc, AnnotationSet sentimentAnnotationSet,
      DocumentMetaData documentMeta) throws Exception {

    AnnotationSet annotatioSentimentSentenceSet = sentimentAnnotationSet;
    
    /**
     * Iterates through all sentences that contains sentiment (pos or neg)
     */
    Iterator<Annotation> annotatioSentimentSentenceSetIt = annotatioSentimentSentenceSet.iterator();
    SentimentDocument currentSentimentDocument = null;

    HibernateConstantEntities constantDBentities = DatabaseFacade.getSingletonFacade()
        .getConstantEntites();

    while (annotatioSentimentSentenceSetIt.hasNext()) {
      Annotation sentLevelObj = (Annotation) annotatioSentimentSentenceSetIt.next();

      SentimentObject so = null;
      SentimentObjectType sot = null;

      SentimentFeatureType sft = null;

      if (sentLevelObj.getFeatures().containsKey("SentimentObjectType")) {
        String sentimentObjectTypesListString = sentLevelObj.getFeatures().get("SentimentObjectType")
            .toString();
        log.debug("SentimentObjectType " + sentimentObjectTypesListString + " extracted");
        SentimentObjectType currentType = null;

        if (!sentimentObjectTypesListString.equalsIgnoreCase("unknown")) {

          String[] objectTypes = splitFeatureString(sentimentObjectTypesListString);

          for (String currentObjectType : objectTypes) {

            currentObjectType = currentObjectType.trim();

            if (currentObjectType.contains("Stock")) {
              sot = DatabaseFacade.getSingletonFacade().getSentimentObjectTypeFromName("Stock");
            } else {
              sot = DatabaseFacade.getSingletonFacade().getSentimentObjectType(currentObjectType);
            }
          }
        }
      }

      // Define Sentiment Object
      so = null;
      if (sentLevelObj.getFeatures().containsKey("SentimentObject")) {
        String sentimentObjectName = sentLevelObj.getFeatures().get("SentimentObject").toString();
        log.debug("SentimentObjectName " + sentimentObjectName + " extracted");
        String sentimentObjectOntologyURI = sentLevelObj.getFeatures().get("SentimentObjectURI")
            .toString();
        so = DatabaseFacade.getSingletonFacade().createOrgetSO(sot, sentimentObjectName,
            sentimentObjectOntologyURI);
      }

      // Define Sentiment Feature
      String sentimentObjectOntologyURI = null;
      
      sft = null;
      if (sentLevelObj.getFeatures().containsKey("ObjectFeature")) {
        FeatureMap sentObjectFeatures = sentLevelObj.getFeatures();
        String currentFeatureName = sentObjectFeatures.get("ObjectFeature").toString();
        log.debug("ObjectFeature " + currentFeatureName + " extracted");
        currentFeatureName = GlobalParameters.changeFeatureName(currentFeatureName);
        if (sentLevelObj.getFeatures().containsKey("ObjectFeatureURI")) {
          sentimentObjectOntologyURI = sentObjectFeatures.get("ObjectFeatureURI").toString();
        }
        sft = DatabaseFacade.getSingletonFacade().createOrgetSF(currentFeatureName,
            sentimentObjectOntologyURI);
      }

      if (so != null && sft != null) {
        // Assign sentiment sentences to sentiment documents
        Map<Integer, SentimentSentence> sentimentSentenceMapInDoc = null;

        currentSentimentDocument = classifiedDocument.existsSentimentDocument(so, sft);

        if (currentSentimentDocument != null) {
          sentimentSentenceMapInDoc = currentSentimentDocument.getSentimentSentenceMap();
        } else {

          // Create new Document Level Sentiment
          sentimentSentenceMapInDoc = new HashMap<Integer, SentimentSentence>();

          currentSentimentDocument = new SentimentDocument(so, sft,
              constantDBentities.sentimentLevelDocument, constantDBentities.classifierTypeCrisp,
              documentMeta);
          ArrayList<SentimentDocument> extractedSentimentDocuments = classifiedDocument
              .getSentimentDocuments();
          extractedSentimentDocuments.add(currentSentimentDocument);
        }

        /* SENTENCES */
        SentimentSentence currentSentenceLevel = this.defineSentenceLevel(doc, sentLevelObj, so, sft,
            constantDBentities.sentimentLevelSentence, constantDBentities.classifierTypeCrisp,
            documentMeta);

        if (currentSentenceLevel != null) {
          sentimentSentenceMapInDoc.put(Integer.valueOf(sentLevelObj.getId()), currentSentenceLevel);
          currentSentimentDocument.setSentimentSentencesMap(sentimentSentenceMapInDoc);
        }
      }else{
        log.info("SO or SF is null -> throw new Exception");
        Exception ex = new Exception("Sentimentobject or SentimentFeature is null in current Sentence");
        throw ex;
      }
    }

    try {

      calculateDocSentimentScore(doc);

      if (EXECUTE_FUZZY) {
        NewApproachHybridFuzzy fuzzyClassifier = new NewApproachHybridFuzzy();
        fuzzyClassifier.run(doc, classifiedDocument);
      }
      
    } catch (HibernateException hibEx) {
      log.error("Cannot update Sentiment on docLevel");
      throw hibEx;
    } catch (Exception e) {
      log.error(e.getClass().getName() + " occured on fuzzy classificton");
      throw e;
    }
  }

  private SentimentSentence defineSentenceLevel(Document doc, Annotation sentenceLevelAnnotation,
      SentimentObject so, SentimentFeatureType sft, SentimentLevelDefinition sentimentLevelSentence,
      SentimentClassifierType classifierTypeCrisp, DocumentMetaData docMeta) {

    /* Extract needed sentence information */

    // Start and End Node
    Long startNode = sentenceLevelAnnotation.getStartNode().getOffset();
    Long endNode = sentenceLevelAnnotation.getEndNode().getOffset();
    DocumentContent docContent = doc.getContent();

    // Text
    String text = docContent.toString().substring(startNode.intValue(), endNode.intValue());

    // Polarity
    Integer sentimentScore = null;
    String sentimentPolarity = new String();

    // Subfeature
    SentimentFeatureType sentimentSubFeature = null;

    // Indicator Type
    IndicatorType inditype = null;
    String indicatortype = new String();

    // Indicator
    Indicator indicatorConcept = null;
    String indicatorname = new String();
    String indicatorURI = new String();

    // Sentiment Indicator
    SentimentIndicator sentimentIndicator = null;
    Long indicatorScore = null;
    int indicatorCorrelation = 0;

    SentimentSentence currentSentenceLevel = null;

    // Extract Information only if sentiment polarity of the sentiment is defined
    if (sentenceLevelAnnotation.getFeatures().containsKey("SentimentPolarity")) {
      sentimentScore = null;
      sentimentPolarity = sentenceLevelAnnotation.getFeatures().get("SentimentPolarity").toString();
      sentimentScore = scoreSentiment(sentimentPolarity);

      // Extract Subfeature
      if (sentenceLevelAnnotation.getFeatures().containsKey("SubFeature")) {

        String subFeatureName = sentenceLevelAnnotation.getFeatures().get("SubFeature").toString();
        log.debug("Feature: \"SubFeature\" in sentenceLevelAnnotation extracted: " + subFeatureName);

        /*
         * TODO: Is it possible, that the Name of a Subfeature repeatet twice in
         * database? yes -> change upDateSentimentFeatureType(subFeatureName) to
         * the combination of the OntologyURI and the Name
         */
        String subFeatureURI = null;
        if (sentenceLevelAnnotation.getFeatures().containsKey("SubFeatureURI")) {
          subFeatureURI = sentenceLevelAnnotation.getFeatures().get("SubFeatureURI").toString();
          log.debug("Feature: \"SubFeatureURI\" in sentenceLevelAnnotation extracted: " + subFeatureURI);
        }
        // Save Subfeature to the DB
        sentimentSubFeature = DatabaseFacade.getSingletonFacade().createOrgetSF(subFeatureName,
            subFeatureURI);
      }

      // Extract Indicator Type
      if (sentenceLevelAnnotation.getFeatures().containsKey("IndicatorType")) {
        indicatortype = sentenceLevelAnnotation.getFeatures().get("IndicatorType").toString();
        inditype = DatabaseFacade.getSingletonFacade().createIndicatorType(indicatortype);

      }
      // Extract Indicator
      if (sentenceLevelAnnotation.getFeatures().containsKey("IndicatorPhrase")) {
        String indicatorPhraseIDString = sentenceLevelAnnotation.getFeatures().get("IndicatorPhrase")
            .toString();
        Integer indicatorPhraseID = Integer.parseInt(indicatorPhraseIDString);
        Annotation indiPhraseAnnot = doc.getAnnotations("Indicator").get(indicatorPhraseID);
        indicatorname = indiPhraseAnnot.getFeatures().get("name").toString();
        indicatorURI = indiPhraseAnnot.getFeatures().get("URI").toString(); // -> URI oder INDICATOR CONCEPT URI
        indicatorConcept = DatabaseFacade.getSingletonFacade().createIndicatorConcept(indicatorname,
            indicatorURI);
      }
      // Extract Sentiment Indicator
      if (sentenceLevelAnnotation.getFeatures().containsKey("IndicatorCorrelation")) {
        String indicatorCorrelationString = sentenceLevelAnnotation.getFeatures()
            .get("IndicatorCorrelation").toString();
        // Value of Indicator CorrelationDefinition
        if (indicatorCorrelationString.equalsIgnoreCase("positive")) {
          indicatorCorrelation = new Integer(1);
        } else
          if (indicatorCorrelationString.equalsIgnoreCase("negative")) {
            indicatorCorrelation = new Integer(-1);
          }
        // Value of Indicator Score
        if (sentenceLevelAnnotation.getFeatures().containsKey("IndicatorPolarity")) {
          String indicatorPolarity = sentenceLevelAnnotation.getFeatures().get("IndicatorPolarity")
              .toString();
          log.trace("Selected Annotation 'IndicatorPolarity' " + indicatorPolarity);
          if (indicatorPolarity.equalsIgnoreCase("positive")) {
            indicatorScore = new Long(1);
          }
          if (indicatorPolarity.equalsIgnoreCase("negative")) {
            indicatorScore = new Long(-1);
          }
        }
        sentimentIndicator = DatabaseFacade.getSingletonFacade().createOrgetSentimentIndicator(
            indicatorname, indicatorCorrelation, indicatorScore, inditype, indicatorConcept);
      }

      // Create Sentiment Sentence
      currentSentenceLevel = new SentimentSentence(startNode, endNode, so, sft,
          sentimentLevelSentence, classifierTypeCrisp, docMeta, sentimentScore, sentimentPolarity,
          sentimentSubFeature, sentimentIndicator);

      HibernateConstantEntities constantDBentities = databaseConn.getConstantEntites();
      // extract the sentimentsentence text as phrase
      Phrase sentencePhrase = new Phrase(constantDBentities.getSentencePhraseType(), text,
          sentimentScore, startNode.intValue(), endNode.intValue());
      currentSentenceLevel.setSentencePhrase(sentencePhrase);

      // extract sentiment phrase
      if (sentenceLevelAnnotation.getFeatures().containsKey("SentimentPhraseID")) {
        String sentimentPhraseIdStr = sentenceLevelAnnotation.getFeatures().get("SentimentPhraseID")
            .toString();
        Integer sentimentPhraseID = new Integer(sentimentPhraseIdStr);
        Annotation sentimentPhraseAnnot = doc.getAnnotations("SentimentPhrase").get(sentimentPhraseID);
        Phrase sentimentPhrase = extractPhrase(sentimentPhraseAnnot, docContent, "SentimentPolarity",
            constantDBentities.getSentimentPhraseType());
        currentSentenceLevel.setSentimentSentencePhrase(sentimentPhrase);
      }

      // extract orientation phrases
      extractOrientationTermPhrases(doc, sentenceLevelAnnotation, docContent, currentSentenceLevel);

      /* extract the FeaturePhrase */
      if (sentenceLevelAnnotation.getFeatures().containsKey("ObjectFeaturePhrase")) {
        try {
          String featurePhraseIDString = sentenceLevelAnnotation.getFeatures()
              .get("ObjectFeaturePhrase").toString();
          Integer featurePhraseID = Integer.parseInt(featurePhraseIDString);
          Phrase objectFeaturePhrase = extractPhrase(doc.getAnnotations().get(featurePhraseID),
              docContent, "SentimentPolarity", constantDBentities.getFeaturePhraseType());
          currentSentenceLevel.setSentimentFeaturePhrase(objectFeaturePhrase);
        } catch (NumberFormatException nfex) {
          log.error("Cannot parse sentimentObjectFeaturePhrase String containing id");
        }
      }

      // Define direct or indirect
      String typeSentimetnSentenceDirIndir = null;

      if (sentenceLevelAnnotation.getFeatures().containsKey("SentimentSentenceDirect")) {
        // String [direct | indirect]
        typeSentimetnSentenceDirIndir = sentenceLevelAnnotation.getFeatures()
            .get("SentimentSentenceDirect").toString();
        // only for <result-xml> needed
        currentSentenceLevel.setSentimentSentenceType(typeSentimetnSentenceDirIndir);
        /*
         * * Sentiment of the sentence is direct, the sentiment object is
         * mentioned directly in the sentence
         */
        if (typeSentimetnSentenceDirIndir.equalsIgnoreCase("direct")) {

          if (sentenceLevelAnnotation.getFeatures().containsKey("SentimentObjectPhrase")) {
            String sentObjectFeaturePhraseStringID = sentenceLevelAnnotation.getFeatures()
                .get("SentimentObjectPhrase").toString();

            try {
              Integer sentObjectPhraseID = Integer.parseInt(sentObjectFeaturePhraseStringID);
              Annotation sentObjectPhraseAnnot = doc.getAnnotations("Sentiment_Object").get(
                  sentObjectPhraseID);
              Phrase sentimentObjectPhrase = extractPhrase(
                  doc.getAnnotations("Sentiment_Object").get(sentObjectPhraseID), docContent, "",
                  constantDBentities.getObjectPhraseType());
              currentSentenceLevel.setSentimentObjectPhrase(sentimentObjectPhrase);
            } catch (NumberFormatException nfex) {
              log.error("Cannot parse sentimentObjectFeaturePhrase String containing id: "
                  + sentObjectFeaturePhraseStringID);
            }
          }
        } else {

          /*
           * Sentiment of the sentence is indirect, the sentimentObject is not
           * mentioned directly so the sentiment is based/calculated on an
           * indicator
           */
          if (typeSentimetnSentenceDirIndir.equalsIgnoreCase("indirect")) {
            /*
             * * Sentiment of the sentence is indirect, the indicator is
             * mentioned in the sentence
             */
            if (sentenceLevelAnnotation.getFeatures().containsKey("IndicatorPhrase")) {
              String indicatorPhraseIDString = sentenceLevelAnnotation.getFeatures()
                  .get("IndicatorPhrase").toString();
              Integer indicatorPhraseID = Integer.parseInt(indicatorPhraseIDString);
              Annotation indiPhraseAnnot = doc.getAnnotations("Indicator").get(indicatorPhraseID);
              Phrase indicatorPhrase = extractPhrase(
                  doc.getAnnotations("Indicator").get(indicatorPhraseID), docContent, "",
                  constantDBentities.getIndicatorPhraseType());
              currentSentenceLevel.setSentimentIndicatorPhrase(indicatorPhrase);
            }
          }
        }
      }

      currentSentenceLevel.setAllSentimentPhraseRelations();
      //DatabaseFacade.getSingletonFacade().createDBSentimentEntry(currentSentenceLevel.getDbSentenceLevelSentiment(), sentimentSession);
    }
    return currentSentenceLevel;
  }

  /**
   * @param doc
   * @param sentenceLevelAnnotation
   * @param docContent
   * @param currentSentenceLevel
   */
  private void extractOrientationTermPhrases(Document doc, Annotation sentenceLevelAnnotation,
      DocumentContent docContent, SentimentSentence currentSentenceLevel) {

    if (sentenceLevelAnnotation.getFeatures().containsKey("OrientationTermPhrase")) {

      String orienttionTermFeatureString = sentenceLevelAnnotation.getFeatures()
          .get("OrientationTermPhrase").toString();
      String[] orientationTermIDs = splitFeatureString(orienttionTermFeatureString);

      for (String currentID : orientationTermIDs) {
        try {
          log.info("Extract OrientationTermPhrase with currentID: " + currentID);
          currentID = currentID.trim();
          Integer otID = null;
          try {
            otID = Integer.valueOf(currentID);
          } catch (NumberFormatException nfex) {
            log.error("Cannot extract Orientationterm Gate ID from String: " + otID);
            throw new NumberFormatException("Cannot extract Orientationterm Gate ID from String: "
                + otID + "\n" + nfex.getMessage());
          }
          Phrase orientationPhrase = extractPhrase(doc.getAnnotations().get(otID), docContent,
              "Orientation", databaseConn.getConstantEntites().getOrientationPhraseType());
          currentSentenceLevel.getOrientationTermPhraseList().add(orientationPhrase);
        } catch (NumberFormatException nfex) {
          log.error(nfex.getMessage());
          log.error("Continue with next orientationTerm");
          continue;
        }
      }
    }
  }

  private Phrase extractPhrase(Annotation ann, DocumentContent docContent, String polarityFeature,
      PhraseType phraseType) {
    Phrase phrase = null;

    Long startNode = ann.getStartNode().getOffset();
    Long endNode = ann.getEndNode().getOffset();
    String text = null;
    try {
      text = docContent.getContent(startNode, endNode).toString();
      String sentimentPolarity = null;
      Integer sentimentScore = null;
      if (polarityFeature != null) {
        if (ann.getFeatures().containsKey(polarityFeature)) {
          sentimentPolarity = ann.getFeatures().get(polarityFeature).toString();
          sentimentScore = scoreSentiment(sentimentPolarity);
        }
      }
      phrase = new Phrase(phraseType, text, sentimentScore, startNode.intValue(), endNode.intValue());
    } catch (InvalidOffsetException e) {
      log.error("Cannot extract text from sentimentphrase from sentimentsentence ");
    }
    // currentSentenceLevel = extractSentimentPhrase(doc, currentSentenceLevel,
    // sentimentPhraseID);
    return phrase;
  }

  public ClassifiedDocument getClassifiedDocument() {
    return classifiedDocument;
  }

  private String[] splitFeatureString(String sentimentObjectTypesListString) {

    String[] gateFeatureComponents;
    /* Delete first and last square bracket [] */
    if (sentimentObjectTypesListString.startsWith("[") && sentimentObjectTypesListString.endsWith("]")) {
      sentimentObjectTypesListString = sentimentObjectTypesListString.substring(1,
          sentimentObjectTypesListString.length() - 1);

      gateFeatureComponents = sentimentObjectTypesListString.split(",");
    } else {
      gateFeatureComponents = new String[1];
      gateFeatureComponents[0] = sentimentObjectTypesListString;
    }

    return gateFeatureComponents;
  }

  /**
   * aggregate sentence level sentiment scores (-1, +1) expressing crisp
   * polarities (pos, neg) to a real valued document level ratio on the interval
   * [-1;+1]
   * */
  private void calculateDocSentimentScore(Document doc) {
    ArrayList<SentimentDocument> extractedSentimentDocuments = classifiedDocument
        .getSentimentDocuments();

    for (int i = 0; i < extractedSentimentDocuments.size(); i++) {

      SentimentDocument sentDocument = extractedSentimentDocuments.get(i);

      Sentiment dbsent = sentDocument.getDbsentiment();
      long sentID = dbsent.getId();

      float documentScore = sentDocument.calculateSentimentScoreDocument();
      dbsent.setScore(new BigDecimal(documentScore));
      log.info("Update sentiment on docLevel: " + documentScore);

      if (sentDocument.getSentimentScoreDocument() > 0) {
        sentDocument.setSentimentPolarity("Positive");
      } else {
        sentDocument.setSentimentPolarity("Negative");
      }

      log.info("Update Sentiment " + dbsent.getScore() + " for document "
          + dbsent.getDocumentMetaData().getId() + " on level: "
          + dbsent.getSentimentLevelDefinition().getName());
      
      try {
        databaseConn.saveOrUpdateObjectToDatabase(dbsent);
      } catch (HibernateException hibEx) {
        log.error("Cannot update Sentiment on docLevel");
        throw hibEx;
      }
    }
  }

  private Integer scoreSentiment(String sentimentPolarity) {
    int sentimentSentenceScore;
    if (sentimentPolarity.equalsIgnoreCase("positive")) {
      sentimentSentenceScore = 1;
    } else {
      if (sentimentPolarity.equalsIgnoreCase("negative")) {
        sentimentSentenceScore = -1;
      } else {
        sentimentSentenceScore = 0;
      }
    }
    return sentimentSentenceScore;
  }
}