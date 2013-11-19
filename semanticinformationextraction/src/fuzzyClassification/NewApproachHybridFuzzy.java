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
package fuzzyClassification;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Resource;
import gate.annotation.AnnotationSetImpl;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.InvalidOffsetException;
import hibernate.HibernateConstantEntities;
import hibernate.DatabaseFacade;
import hibernate.entities.DegreeOfMembership;
import hibernate.entities.DocumentMetaData;
import hibernate.entities.Sentiment;
import hibernate.entities.SentimentClassifierType;
import hibernate.entities.SentimentFeatureType;
import hibernate.entities.SentimentLevelDefinition;
import hibernate.entities.SentimentObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import utils.GateInitSingleton;
import utils.GlobalParameters;
import zeroMQ.messageQueue.WorkerThread;
import classification.ClassifiedDocument;
import classification.SentimentDocument;

/**
 * Extractrs the fuzzy sentiment on the document level with respect to:
 * 1. sentiment object/ feature combination
 * 2. sentiment label : positive and negative
 * 3. degree of membership for each of the labels:
 *		- No Amount
 *		- Low Amount
 *		- Medium Amount
 *		- Moderate Amount
 *		- Large Amount
 * 
 */

public class NewApproachHybridFuzzy {

  protected static Logger log = Logger.getLogger(NewApproachHybridFuzzy.class);
  private File HYBRID_GAPP = null;
  private CorpusController hybridGapp = null;

  private File CLASSIFY_Apply_GAPP_POS = null;
  public File CLASSIFY_Apply_GAPP_NEG = null;

  boolean isRelevant = true;
  GateInitSingleton gateInit = GateInitSingleton.getInstance();

  private DatabaseFacade databaseConn = DatabaseFacade.getSingletonFacade();
  private HibernateConstantEntities hibernateConstants = null;

  public NewApproachHybridFuzzy() {
    super();

    this.initialize();
  }

  
  /** Initilize the Fuzzy Machine Learning Classifiers by 
  *loading of needed gate processing pipelines
  */ 
  private void initialize() {

    //Load the gate processing pipeline:
	// -- for classification of positive degree of membership
    CLASSIFY_Apply_GAPP_POS = GlobalParameters.getClassify_apply_gapp_Pos();
	// -- for classification of negative degree of membership
    CLASSIFY_Apply_GAPP_NEG = GlobalParameters.getClassify_apply_gapp_Neg();
    // -- for document annotation used for preprocessing before applying the ML classifiers
	HYBRID_GAPP = GlobalParameters.getHybridGateApp();

    try {
      hybridGapp = gateInit.loadApplication(HYBRID_GAPP);
    } catch (PersistenceException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ResourceInstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    this.hibernateConstants = databaseConn.getConstantEntites();
  }

  /**
  * Apply the machine learning fuzzy classifier on document
  * @parameter: sourceDocument: the document to be classified
  * @parameter: classifiedDocument: the information about sentiment object & obejct feature combinations contained in the document 
  */
  
  public void run(Document sourceDocument, ClassifiedDocument classifiedDocument) throws Exception {

	// execute preprocessing using Hybrid_GAPP
    sourceDocument = modifyGeneralAnnotation(sourceDocument);
	
	// Apply fuzzy sentiment classification on document
	// with respect to the sentiment object & obejct feature combination
    ArrayList<SentimentDocument> extractedSentimentDocuments = classifiedDocument
        .getSentimentDocuments();
    for (int i = 0; i < extractedSentimentDocuments.size(); i++) {

      CorpusController applicationPos = null;
      CorpusController applicationNeg = null;
      Corpus corpus = null;
      Document doc = null;

      try {
        SentimentDocument sentDocument = extractedSentimentDocuments.get(i);
        AnnotationSet definedSentiment = null;

        try {
          corpus = Factory.newCorpus("Fuzzy_Corpus");
        } catch (ResourceInstantiationException e) {
          log.debug("Cannot create Corpus" + e.getMessage());
        }
        
        String so = sentDocument.getSentimentObject().getName();
        String sf = sentDocument.getSentimentFeature().getName();

		// Modify the annotations in the document with respect to the sentiment object/feature combination
		// by mean of delete all annotations of sentiment sentences and tokens
		// that do not refer to this object/feature combination
        doc = modifyDocAnnotation(sourceDocument, so, sf);

		// Execute fuzzy classification:
		
        if (isRelevant)
          try {
		  // first with regards to pocitive degree of membership
            synchronized (CLASSIFY_Apply_GAPP_POS) {
              applicationPos = gateInit.loadApplication(CLASSIFY_Apply_GAPP_POS);
            }
			// second with regards to negative degree of membership
            synchronized (CLASSIFY_Apply_GAPP_NEG) {
              applicationNeg = gateInit.loadApplication(CLASSIFY_Apply_GAPP_NEG);
            }
            
            corpus.add(doc);
            applicationPos.setCorpus(corpus);
            applicationPos.execute();
            log.debug("Application :'" + applicationPos.getName() + "' executet on Corpus: '"
                + corpus.getName() + "'");
            applicationNeg.setCorpus(corpus);
            applicationNeg.execute();

            log.debug("Application :'" + applicationNeg.getName() + "' executet on Corpus: '"
                + corpus.getName() + "'");

            definedSentiment = doc.getAnnotations("Output");
            log.debug("AnnotationSet Output for fuzzy classifier extracted");

			// Store infomration about extracted fuzzy sentiment in the DB
            
			Sentiment hybridFuzzySent = new Sentiment();
            log.debug("Created new Sentiment Object on fuzzy-classifier");
			
			// SET sentiment level: document
            SentimentLevelDefinition docLevel = hibernateConstants.getSentimentLevelDocument();
            hybridFuzzySent.setSentimentLevelDefinition(docLevel);
            log.debug("Set Leveldefinition to fuzzy sentiment to: " + docLevel.getName()
                + " with ID: " + docLevel.getId());

			// SET classifier type: FUZZY Hybrid	
            SentimentClassifierType classifierType = hibernateConstants.getClassifierTypeFuzzy();
            hybridFuzzySent.setSentimentClassifierType(classifierType);
            log.debug("Set classifier type to fuzzy sentiment: " + classifierType.getSentimentType()
                + " and method: " + classifierType.getClassifierMethod());

			// SET sentiment object
            SentimentObject sentObj = sentDocument.getSentimentObject();
            hybridFuzzySent.setSentimentObject(sentObj);
            log.debug("Set sentimentObject to fuzzy sentiment with ID: " + sentObj.getId());

			// SET sentiment feature
            SentimentFeatureType feature = sentDocument.getSentimentFeature();
            hybridFuzzySent.setSentimentFeatureType(feature);
            log.debug("Set sentimentFeature to fuzzy sentiment with ID: " + feature.getId());

            // SET sentiment polarity
            log.info("Set pos/neg fuzzy Amount default to 'n/a'");
            DegreeOfMembership dom = FuzzyLabels.NOTDEFINED.getDoM();
            sentDocument.setNegDom(dom);
            hybridFuzzySent.setNegativeDOMs(dom);
            sentDocument.setPosDom(dom);
            hybridFuzzySent.setPositiveDOMs(dom);
            
			// Set sentiment degree of membership
			// This information is stored in document annotation set "Output" under annotations "LabelPos" OR "LabelNeg" as feature "sentiment"
            if (!definedSentiment.isEmpty()) {
              AnnotationSet labelSetPositive = definedSentiment.get("LabelPos");
              log.debug("AnnotationSet with posLabel extracted with size: " + labelSetPositive.size());
              Iterator<Annotation> labelPosSetIT = labelSetPositive.iterator();

              AnnotationSet labelSetNegative = definedSentiment.get("LabelNeg");
              log.debug("AnnotationSet with negLabel extracted with size: " + labelSetNegative.size());
              Iterator<Annotation> labelNegSetIT = labelSetNegative.iterator();

              // Extratc information about positive label
              while (labelPosSetIT.hasNext()) {
                Annotation labelPos = labelPosSetIT.next();
                FeatureMap labelPosFeature = labelPos.getFeatures();
                String positive = labelPosFeature.get("sentiment").toString();
                log.info("Positive gate Annotation extracted: " + positive);
                positive = positive.substring(0, positive.indexOf("OfPositivity"));

                for (FuzzyLabels l : FuzzyLabels.values()) {
                  DegreeOfMembership dom1 = l.getDoM();
                  if (l.getGateLabelName().equalsIgnoreCase(positive)) {
                    hybridFuzzySent.setPositiveDOMs(dom1);
                    sentDocument.setPosDom(dom1);
                    log.debug("Set positive DOM FK with ID: " + dom1.getId() + " and label: "
                        + dom1.getLabel() + " to sentiment with id: " + hybridFuzzySent.getId());
                    break;
                  }
                }
              }

              // Extratc information about negative label
			  while (labelNegSetIT.hasNext()) {
                Annotation labelNeg = labelNegSetIT.next();
                FeatureMap labelNegFeature = labelNeg.getFeatures();
                String negative = labelNegFeature.get("sentiment").toString();
                log.info("Negative gate Annotation extracted: " + negative);
                negative = negative.substring(0, negative.indexOf("OfNegativity"));

                for (FuzzyLabels l : FuzzyLabels.values()) {
                  DegreeOfMembership dom1 = l.getDoM();
                  if (l.getGateLabelName().equalsIgnoreCase(negative)) {
                    hybridFuzzySent.setNegativeDOMs(dom1);
                    sentDocument.setNegDom(dom1);
                    log.debug("Set negative DOM FK with ID: " + dom1.getId() + " and label: "
                        + dom1.getLabel() + " to sentiment with id: " + hybridFuzzySent.getId());
                    break;
                  }
                }
              }
            }
            
			// Save infomration about extracted sentiment in the DB
            DocumentMetaData docMeta = classifiedDocument.getDocMeta();
            hybridFuzzySent.setDocumentMetaData(docMeta);
            docMeta.getSentiments().add(hybridFuzzySent);
            log.debug("Set DocumentMetaDataObject to fuzzy sentiment with ID: " + docMeta.getId());
            databaseConn.saveOrUpdateObjectToDatabase(docMeta);

          } catch (ExecutionException e) {
            log.debug(e.getClass().getName() + " occured on hybrid fuzzy approach run()");
            log.debug(e.getMessage());
            throw e;
          } catch (PersistenceException e) {
            log.debug(e.getClass().getName() + " occured on hybrid fuzzy approach run()");
            log.debug(e.getMessage());
            throw e;
          } catch (ResourceInstantiationException e) {
            log.debug(e.getClass().getName() + " occured on hybrid fuzzy approach run()");
            log.debug(e.getMessage());
            throw e;
          } catch (IOException e) {
            log.debug(e.getClass().getName() + " occured on hybrid fuzzy approach run()");
            log.debug(e.getMessage());
            throw e;
          }
      } catch (Exception ex) {
        log.error(ex.getClass().getName() + " occured on running fuzzy classification");
        log.error(ex.getMessage());
        throw ex;

	// free the Heap Space 
      } finally {
        applicationPos.setCorpus(null);
        applicationNeg.setCorpus(null);
        hybridGapp.setCorpus(null);
        
        GateInitSingleton.executeResetApplication(doc);
        GateInitSingleton.unloadGateResources(doc, corpus);

        Factory.deleteResource(applicationPos);
        Factory.deleteResource(applicationNeg);
        Factory.deleteResource(hybridGapp);
      }
    }
  }

  /**
   * Preprocesses the document in order to create annotations used 
   * as input for fuzzy classifiers
   * @param sourceDocument: document to be processed
   * @return changed sourceDocument: document with modified annotations 
   * @throws ResourceInstantiationException
   */
  private Document modifyGeneralAnnotation(Document sourceDocument) {
    Corpus corpus = null;
    try {
      corpus = Factory.newCorpus("modifyFuzzy");
    } catch (ResourceInstantiationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    corpus.add(sourceDocument);

    hybridGapp.setCorpus(corpus);
    try {
      hybridGapp.execute();
    } catch (ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

	// Create empty document annotations. These annotations are used 
	// as plaseholder for infomration 
	// about fuzzy sentiments with regard to sentiment label
    sourceDocument = labelGateDoc("LabelPos", sourceDocument);
    sourceDocument = labelGateDoc("LabelNeg", sourceDocument);

    GateInitSingleton.unloadGateResources(corpus);

    return sourceDocument;
  }

  /** Delete all annotation of sentiment sentences inlcuding the contained annotations 
  * that do not refer to the predefined sentiment object/feature combination 
  * @param docOriginal: document to be processed
  * @param so: sentiment object
  * @param of: object feature
  * @return doc: modified document concerning the annotations
  */
  private Document modifyDocAnnotation(Document docOriginal, String so, String of)
      throws ResourceInstantiationException {
    isRelevant = true;
    Document doc = (Document) Factory.duplicate(docOriginal);
    AnnotationSet docAnn = doc.getAnnotations();
    docAnn.addAll(new AnnotationSetImpl(docOriginal.getAnnotations()));

    AnnotationSet allSSAnnSet = docAnn.get("ALLSentimentSentence");
    AnnotationSet otSetToken = docAnn.get("ALLToken");
    Iterator<Annotation> allSSAnnIT = allSSAnnSet.iterator();
	
	// Itearate through all sentiment sentences in the document 
	// and delete sentiment sentences and contained tokens that 
	// do not refer to the sentiment object feature combination
    while (allSSAnnIT.hasNext()) {
      Annotation allSSann = (Annotation) allSSAnnIT.next();
      FeatureMap allSSfeature = allSSann.getFeatures();
      String soCurrent = allSSfeature.get("SO").toString();
      String ofCurrent = allSSfeature.get("OF").toString();
      Long sn = allSSann.getStartNode().getOffset();
      Long en = allSSann.getEndNode().getOffset();
      if (soCurrent.equalsIgnoreCase(so) & ofCurrent.equalsIgnoreCase(of)) {
        allSSfeature.put("relevance", "yes");
        AnnotationSet otSet = otSetToken.getContained(sn, en);
        Iterator<Annotation> otSetIT = otSet.iterator();
        while (otSetIT.hasNext()) {
          Annotation ot = otSetIT.next();
          FeatureMap otFeature = ot.getFeatures();
          if (otFeature.containsKey("relevance")) {
            otFeature.put("relevance", "yes");
          }
        }
      } else {
        docAnn.remove(allSSann);
      }
    }

    Iterator<Annotation> otSetTokenIT = otSetToken.iterator();
    while (otSetTokenIT.hasNext()) {
      Annotation tokenAnn = (Annotation) otSetTokenIT.next();
      FeatureMap allTOfeature = tokenAnn.getFeatures();
      if (allTOfeature.containsKey("relevance")) {
        if ((allTOfeature.get("relevance").toString()).equalsIgnoreCase("no")) {
          docAnn.remove(tokenAnn);
        }
      }
    }
    allSSAnnSet = docAnn.get("ALLSentimentSentence");
    otSetToken = docAnn.get("ALLToken");

    if (allSSAnnSet.isEmpty() || otSetToken.isEmpty()) {
      isRelevant = false;
    }

    return doc;
  }

  
  /**
  * Creates empty annotation in the document that are used by 
  * machine learning classifiers as placeholders for output annotations
  */
  
  private static Document labelGateDoc(String annotationName, Document doc) {
    // create Annotation of document content that is equal the label of doc (positive, negative)

    AnnotationSet docAnnotations = doc.getAnnotations();

    long docEndNode = doc.getContent().size();

    long docFirstNode = (long) 0;

    try {
      FeatureMap fm = Factory.newFeatureMap();
      fm.put("sentiment", "unknown");
      fm.put("so", "unknown");
      fm.put("of", "unknown");
      docAnnotations.add(docFirstNode, docEndNode, annotationName, fm);
    } catch (InvalidOffsetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return doc;
  }

}
