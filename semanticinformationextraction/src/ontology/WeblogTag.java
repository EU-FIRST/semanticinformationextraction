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
package ontology;

import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.creole.ontology.AnnotationProperty;
import gate.creole.ontology.DatatypeProperty;
import gate.creole.ontology.Literal;
import gate.creole.ontology.OInstance;
import gate.creole.ontology.OResource;
import gate.creole.ontology.OURI;
import gate.creole.ontology.ObjectProperty;
import gate.creole.ontology.Ontology;
import gate.creole.ontology.RDFProperty;
import gate.util.GateException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javassist.bytecode.Descriptor.Iterator;

import org.apache.log4j.Logger;

import utils.GateInitSingleton;
import utils.GlobalParameters;

/**
 * In order to extract the meta information about a web-site,
 * the HTML-tags refering the meta-data have to be specified.
 * This infomration is stored in the onotlogy and is loaded (as singleton)
 * in dependence on the web-page from which the infomration have to be extracted.
 * 
 */
public class WeblogTag {
  private static Logger log = Logger.getLogger(WeblogTag.class.getName());
  
//Title of the web-page
 private ArrayList<String> titleTags = new ArrayList<String>();
 // The main content (article) of the web page 
 private ArrayList<String> textTags = new ArrayList<String>();
 // Publication Date
 private ArrayList<String> dateTags = new ArrayList<String>();
 // The name of the blog/site
 private ArrayList<String> blognameTags = new ArrayList<String>();
 // The author name
 private ArrayList<String> authorTags = new ArrayList<String>();


  private String dFormat = "";
  private static Properties configProperties = GlobalParameters.loadConfigFile();
  private File ontologyFile = null;

  private Ontology onto = null;
  private static WeblogTag singletonWeblogTagInstance = null;

  //Loads the onotology using GATE-Embedded
  private WeblogTag() {
    ontologyFile = GlobalParameters.getOntologyFile();

    File ontoHome = new File(Gate.getPluginsHome(), "Ontology");

    try {
      Gate.getCreoleRegister().addDirectory(ontoHome.toURI().toURL());
    } catch (MalformedURLException e) {
      log.error("Cannot add Ontology to Gate creoleRegister" + e.getMessage());
    }

    // load ontology
    FeatureMap fm = Factory.newFeatureMap();

    try {
      ontologyFile = ontologyFile.getCanonicalFile();
    } catch (IOException e1) {
      log.error(e1.getMessage());
    }

    try {
      fm.put("rdfXmlURL", ontologyFile.toURI().toURL());
    } catch (MalformedURLException e1) {
      log.error(e1.getMessage());
    }

    try {
      onto = (Ontology) Factory.createResource("gate.creole.ontology.impl.sesame.OWLIMOntology", fm);

    } catch (ResourceInstantiationException e) {
      log.error("Cannot create Ontology Resource");
      log.error(e.getMessage());
    }
  }

  /**
   * Specifies the HTML-Tags used by certain web-page
   * The instance of the blog have to be stored in the ontology 
   * @parameter blog: the name of the blog or blog platform from which the meta infomration have to be extracted
 */
  public void loadBlogTag(String blog) {
    OInstance blogin = (OInstance) onto.getOResourceByName(blog);

    Set<gate.creole.ontology.DatatypeProperty> property = blogin.getSetDatatypeProperties();
    java.util.Iterator<gate.creole.ontology.DatatypeProperty> properties = property.iterator();
    while (properties.hasNext()) {

      gate.creole.ontology.DatatypeProperty nextproperty = (gate.creole.ontology.DatatypeProperty) properties
          .next();

      String propertyname = nextproperty.getURI().getResourceName();

   // Extract Weblog Name Tags
      // ................................................................................................

      if (propertyname.equalsIgnoreCase("blognametag")) {

        List<gate.creole.ontology.Literal> values1 = blogin.getDatatypePropertyValues(nextproperty);
        if (!values1.isEmpty()) {
          java.util.Iterator<gate.creole.ontology.Literal> values = values1.iterator();
          while (values.hasNext()) {
            gate.creole.ontology.Literal val = values.next();
            String value = val.getValue();
            value = value.replace(".", "=");
            blognameTags.add(value);

          }
        }
      }

   // Extract Date Tags
      // ................................................................................................

      if (propertyname.equalsIgnoreCase("datetag")) {

        List<gate.creole.ontology.Literal> values1 = blogin.getDatatypePropertyValues(nextproperty);
        if (!values1.isEmpty()) {
          java.util.Iterator<gate.creole.ontology.Literal> values = values1.iterator();
          while (values.hasNext()) {
            gate.creole.ontology.Literal val = values.next();
            String value = val.getValue();
            value = value.replace(".", "=");
            dateTags.add(value);

          }
        }
      }

      // Extract Article Title Tags
      // .............................................................................................

      if (propertyname.equalsIgnoreCase("titletag")) {

        List<gate.creole.ontology.Literal> values1 = blogin.getDatatypePropertyValues(nextproperty);
        if (!values1.isEmpty()) {
          java.util.Iterator<gate.creole.ontology.Literal> values = values1.iterator();
          while (values.hasNext()) {
            gate.creole.ontology.Literal val = values.next();
            String value = val.getValue();
            value = value.replace(".", "=");
            titleTags.add(value);

          }
        }
      }

      // Extract Author Name Tags
      // ................................................................................................

      if (propertyname.equalsIgnoreCase("authortag")) {

        List<gate.creole.ontology.Literal> values1 = blogin.getDatatypePropertyValues(nextproperty);
        if (!values1.isEmpty()) {
          java.util.Iterator<gate.creole.ontology.Literal> values = values1.iterator();
          while (values.hasNext()) {
            gate.creole.ontology.Literal val = values.next();
            String value = val.getValue();
            value = value.replace(".", "=");
            authorTags.add(value);

          }
        }
      }

      // Extarct Article Text Tags
      // ................................................................................................

      if (propertyname.equalsIgnoreCase("texttag")) {
        List<gate.creole.ontology.Literal> values1 = blogin.getDatatypePropertyValues(nextproperty);
        if (!values1.isEmpty()) {
          java.util.Iterator<gate.creole.ontology.Literal> values = values1.iterator();
          while (values.hasNext()) {
            gate.creole.ontology.Literal val = values.next();
            String value = val.getValue();
            value = value.replace(".", "=");
            textTags.add(value);

          }
        }
      }

   // Extract Infomration about Date Format used by the web-page
      // ................................................................................................

      if (propertyname.equalsIgnoreCase("dateformat")) {
        List<gate.creole.ontology.Literal> values1 = blogin.getDatatypePropertyValues(nextproperty);
        if (!values1.isEmpty()) {
          java.util.Iterator<gate.creole.ontology.Literal> values = values1.iterator();
          while (values.hasNext()) {
            gate.creole.ontology.Literal val = values.next();
            String value = val.getValue();
            dFormat = value;

          }
        }
      }

    }
  }

  /**
   * Specifies the Version of the used ontology
   * from the VersionInfo field
   * 
   * @return versionInfo Tag from ontology
   */
  public synchronized String loadVersionInfo() {
    String version = "";

    Set<AnnotationProperty> annotProperties = onto.getAnnotationProperties();

    int i = 0;
    for (AnnotationProperty ap : annotProperties) {

      if (ap.getName().equalsIgnoreCase("versionInfo")) {
        List<Literal> values = onto.getOntologyAnnotationValues(ap);
        version = values.get(0).toString();
        break;
      }
    }
    log.debug("Ontology versionInfo: " + version + " extracted");

    return version;
  }

  public ArrayList<String> getTitle() {
    return (this.titleTags);
  }

  public String getDFormat() {
    return (this.dFormat);
  }

  public ArrayList<String> getDate() {
    return (this.dateTags);
  }

  public ArrayList<String> getText() {
    return (this.textTags);
  }

  public ArrayList<String> getBlogname() {
    return (this.blognameTags);
  }

  public synchronized File getOntologyFile() {
    return ontologyFile;
  }

  public void setOntologyFile(File ontologyFile) {
    this.ontologyFile = ontologyFile;
  }

  public ArrayList<String> getAuthor() {
    return (this.authorTags);
  }

  public Ontology getOnto() {
    return onto;
  }

  //The Tags refering a certain web-log are loaded as singleton
  public synchronized static WeblogTag getInstance() {

    if(singletonWeblogTagInstance == null){
      log.debug("Creating WeblogTag-Instance as singleton");
      singletonWeblogTagInstance = new WeblogTag();
    }else{
      log.debug("WeblogTag is initialized. Return current instance");
    }

    return singletonWeblogTagInstance;
  }

}
