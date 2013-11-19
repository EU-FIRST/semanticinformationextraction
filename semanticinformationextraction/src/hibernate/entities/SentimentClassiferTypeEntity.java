package hibernate.entities;

import hibernate.DatabaseFacade;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

public class SentimentClassiferTypeEntity extends SentimentClassifierType {

  private static Logger log = Logger.getLogger(DatabaseFacade.class.getName());
  
  public static SentimentClassifierType loadSentimentClassifierType(String classifierMethod, String sentimentType) {

    SentimentClassifierType type = null;
    
    Session hibernateSession = DatabaseFacade.getSingletonFacade().getHibernateSession();
    Criteria crit = hibernateSession.createCriteria(SentimentClassifierType.class);
    crit.add(Restrictions.eq("classifierMethod", classifierMethod).ignoreCase());
    crit.add(Restrictions.eq("sentimentType", sentimentType));
    crit.addOrder(Order.asc("id"));

    List<SentimentClassifierType> currentClassifierTypeList = crit.list();
    if (currentClassifierTypeList.size() >= 1) {
      type = currentClassifierTypeList.get(0);
    } else {
      log.error("There are no SentimentClassifierType in database with classifierMethod: "
          + classifierMethod + " and sentimentType: " + sentimentType);

      SentimentClassifierType newClassifierType = new SentimentClassifierType();
      newClassifierType.setClassifierMethod(classifierMethod);
      newClassifierType.setSentimentType(sentimentType);

    }
    return type;
  }
  
}
