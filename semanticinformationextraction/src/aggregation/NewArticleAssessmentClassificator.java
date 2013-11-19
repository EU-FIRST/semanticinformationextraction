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
package aggregation;

import gate.util.InvalidOffsetException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import classification.SentimentSentence;

/**
 * Aggreages the object specific sentiment on the document level. The document
 * level sentiment is calucalted as following: (positve_sentiment_sentences -
 * negative_sentiment_sentence)/ (positve_sentiment_sentences +
 * negative_sentiment_sentence
 * 
 * @author oaltuntas
 * 
 */
public class NewArticleAssessmentClassificator {

  // Score of positive/negative sentiment sentences in document
  private float posSentimentFactorScore = 0.0f;
  private float negSentimentFactorScore = 0.0f;

  // Sentiment Investor Score of the ClassifiedDocument
  private float score_document = (float) 0.0;

  public NewArticleAssessmentClassificator() {

  }

  /**
   * 
   * @param docSentences
   *          - a list of sentence level sentiments reagading specific sentiment
   *          object
   * @return a score in the range (-1) to (+1) representing the aggregated
   *         document level sentiment
   * @throws InvalidOffsetException
   */
  public float extractOpinion(Map<Integer, SentimentSentence> docSentences)
      throws InvalidOffsetException {
    posSentimentFactorScore = 0.0f;
    negSentimentFactorScore = 0.0f;
    score_document = 0.0f;

    // Check if the parameters for the extra weightings were passed
    if (!docSentences.isEmpty()) {
      Collection<SentimentSentence> values = docSentences.values();
      Iterator it = values.iterator();

      while (it.hasNext()) {
        SentimentSentence currentSentence = (SentimentSentence) it.next();

        String sentiment = currentSentence.getSentimentPolarity();
        if (sentiment.equalsIgnoreCase("positive")) {
          posSentimentFactorScore++;
        }
        if (sentiment.equalsIgnoreCase("negative")) {
          negSentimentFactorScore++;
        }

      }

      score_document = (posSentimentFactorScore - negSentimentFactorScore)
          / (posSentimentFactorScore + negSentimentFactorScore);

    }

    return score_document;
  }
}
