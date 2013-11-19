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
package performance;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.SimpleLayout;

/** measure min,max,avg,median run time and also throughput 
 *  of an arbitrary (processing) loop
  * @author S. Frech */
public class PerformanceMeasurement {

    private Logger workerThreadLogger =Logger.getLogger("PerformanceMeasurement");
    private double laufzeitEregebnisMean;
	private double laufzeitErgebnisStdev;
	private double laufzeitErgebnisMedian;
	private long laufzeitErgebnisMin;
	private long laufzeitErgebnisMax;
	private double anzahlLoopsPerMinute;
	private double anzahlLoopsPerHour;
	private double anzahlLoopsPerDay;
	private static ArrayList<Long> laufzeitErgebnisse = new ArrayList<Long>(50000); //initialize for 50000 loops
	private long startTime;
	private long endTime;
	private int loopCounter = 0;
	private long totalTime = 0;
    private long preprocessingRunTime;
    private long classificationRunTime;
    	
	static double calcMinutes = 1000*60;
    static double calcHours = 1000*60*60;
    static double calcDays = calcHours * 24;
    private static HashMap<String,Long> writeFileTimes = new HashMap<String, Long>();
	
  public PerformanceMeasurement(Logger logger, Level loglevel) throws IOException {
    workerThreadLogger = logger;
    logger.setLevel(loglevel);
  }
  
  /**
   * If there`s no logger read and initialize from log4j-file 
   */
  public PerformanceMeasurement(){
    
    if(workerThreadLogger == null){    
      workerThreadLogger = Logger.getLogger("PerformanceMeasurement");
     
      try {
          SimpleLayout layout = new SimpleLayout();
          ConsoleAppender consoleAppender = new ConsoleAppender(layout);
          workerThreadLogger.addAppender(consoleAppender);
          workerThreadLogger.setLevel(Level.INFO);
        } catch(Exception e) {
          System.out.println(e.getMessage());
        }
    }
  }
	
  /**
   * each Thread should have own log-file
   * @param workerName - Name of the Thread
   * @throws IOException
   */
	public PerformanceMeasurement(String workerName) throws IOException {
	  workerThreadLogger = Logger.getLogger(workerName);
	  
	  if(workerThreadLogger != null){    
	    
	    File workerLogFileName = new File("logs"+ File.separator + "TimeMeasurement_" + workerName + ".log");
	    
	    if(!workerLogFileName.exists()){
	         workerLogFileName.createNewFile();
	        }
 
	    PatternLayout layout = new PatternLayout("%d [%t] [%-5p] %C{6} (%F:%L) - %m%n");

		String datePattern = "'.'yyyy-MM-dd";
		FileAppender appender = new DailyRollingFileAppender(layout, workerLogFileName.getCanonicalPath(), datePattern );
        workerThreadLogger.addAppender(appender);      
        
      }else{
        try {
          SimpleLayout simpleLayout = new SimpleLayout();
          ConsoleAppender consoleAppender = new ConsoleAppender(simpleLayout);
          workerThreadLogger.addAppender(consoleAppender);
          workerThreadLogger.setLevel(Level.INFO);
        } catch(Exception e) {
          System.out.println(e.getMessage());
        }
      }  
	}
	
	/** 
	 * this is to be called on the beginning of a loop 
	 */
	public void startTimeMeasurementLoop() {
		this.startTime = new Date().getTime();
	}
	  	
	/** this is to be called on the end of a loop */
	public void endTimeMeasurementLoop() {
		this.endTime =  new Date().getTime();
		long timeDifference = this.endTime - this.startTime;
		laufzeitErgebnisse.add(timeDifference);
		
		this.loopCounter++;
		this.totalTime += timeDifference;		
	}
	
	public void printRunTimeResults() {
		calcualteRuntimeResults();
		workerThreadLogger.info("Run time per loop (in ms) - Mean: " + String.valueOf(this.laufzeitEregebnisMean));
		workerThreadLogger.info("Run time per loop (in ms) - Stdev: " + String.valueOf(this.laufzeitErgebnisStdev));
		workerThreadLogger.info("Run time per loop (in ms) - Median: " + String.valueOf(this.laufzeitErgebnisMedian));
		workerThreadLogger.info("Run time per loop (in ms) - Min: " + String.valueOf(this.laufzeitErgebnisMin));
		workerThreadLogger.info("Run time per loop (in ms) - Max: " + String.valueOf(this.laufzeitErgebnisMax));
	}
	
	public void printMemoryResults(){
	  workerThreadLogger.info("Printing memory results");
	  workerThreadLogger.info("Number of total loops: " + this.loopCounter);
	  workerThreadLogger.info("Current total HeapSpace Size: " + Runtime.getRuntime().totalMemory());
	  workerThreadLogger.info("Current free HeapSpace Size: " + Runtime.getRuntime().freeMemory());	  
	}
	
	public void printThroughputResults() {
		calculateThroughputResults();
		workerThreadLogger.info("Number of total loops: " + this.loopCounter);
		workerThreadLogger.info("Total time of all loops (min): " + this.totalTime/60000);
		workerThreadLogger.info("Throughput - Loops Per Minute: " + String.valueOf(this.anzahlLoopsPerMinute));
		workerThreadLogger.info("Throughput - Loops Per Hour: " + String.valueOf(this.anzahlLoopsPerHour));
		workerThreadLogger.info("Throughput - Loops Per Day: " + String.valueOf(this.anzahlLoopsPerDay));
	}
	
	public void clearMeasurements() {
		this.laufzeitEregebnisMean = Double.NaN;
		this.laufzeitErgebnisStdev = Double.NaN;
		this.laufzeitErgebnisMedian = Double.NaN;
		this.laufzeitErgebnisMin = 0;
		this.laufzeitErgebnisMax = 0;
		this.anzahlLoopsPerMinute = 0;
		laufzeitErgebnisse = null;
		this.loopCounter = 0;
		this.startTime = 0;
		this.endTime = 0;
	}
	
	public void calcualteRuntimeResults() {
		this.laufzeitEregebnisMean = meanValue();
		this.laufzeitErgebnisStdev = stdevValue();
		this.laufzeitErgebnisMedian = medianValue();
		this.laufzeitErgebnisMin = minValue();
		this.laufzeitErgebnisMax = maxValue();
	}
		
	public void calculateThroughputResults() {				
		double minutes = this.totalTime/calcMinutes;	
		double hours = this.totalTime/calcHours;
		double days= this.totalTime/calcDays;
		
		this.anzahlLoopsPerMinute = this.loopCounter/minutes;
		this.anzahlLoopsPerHour = this.loopCounter/hours;
		this.anzahlLoopsPerDay = this.loopCounter/days;
	}

	public long maxValue() {
		long max = laufzeitErgebnisse.get(0);
		for (int i = 0; i < laufzeitErgebnisse.size(); i++) {
			if (laufzeitErgebnisse.get(i) > max) {
				max = laufzeitErgebnisse.get(i);
			}
		}
		return max;
	}
	
	public long minValue() {
		long min = laufzeitErgebnisse.get(0);
		for (int i = 0; i < laufzeitErgebnisse.size(); i++) {
			if (laufzeitErgebnisse.get(i) < min) {
				min = laufzeitErgebnisse.get(i);
			}
		}
		return min;
	}	
	
	public long sumValues() {
		long sum = 0;
		for (int i = 0; i < laufzeitErgebnisse.size(); i++) {
			sum += laufzeitErgebnisse.get(i);
		}
		return sum;
	}
	
	public double meanValue() {
		int size= laufzeitErgebnisse.size();
		double mean=0;
		if (size>0) mean = this.totalTime/size;
		return mean;
	}	
	
	public double medianValue() {
		double median = 0;
		Collections.sort(laufzeitErgebnisse);  
		if (laufzeitErgebnisse.size() % 2 != 0) {
	    	int m = (int) (laufzeitErgebnisse.size()/2 + 0.5);
	        median = laufzeitErgebnisse.get(m);
	    }
	    else {
	    	int m1=0;
	    	int m2=0;
	    	if (laufzeitErgebnisse.size()>=3){
	    		m1 = (int) (laufzeitErgebnisse.size()/2);
	    		m2 = (int) (laufzeitErgebnisse.size()/2 + 1);
	    	}	
	        median = (laufzeitErgebnisse.get(m1) + laufzeitErgebnisse.get(m2))/2;
	     }
		return median;
	}
	
    public double stdevValue(){
    	double meanValue = meanValue();
    	double stdev = 0.0;
    	int size=laufzeitErgebnisse.size();
    	if (size>0) {
	    	for(int i=0; i < size; i++){
	    		stdev += (laufzeitErgebnisse.get(i)-meanValue)*(laufzeitErgebnisse.get(i)-meanValue);
	    	}
	    	stdev=Math.sqrt(stdev/(double)(size-1));
    	}
	    return stdev;
    }
	
	public double getLaufzeitEregebnisMean() {
		return laufzeitEregebnisMean;
	}
	
	public void setLaufzeitEregebnisMean(double laufzeitEregebnisMean) {
		this.laufzeitEregebnisMean = laufzeitEregebnisMean;
	}
	
	public double getLaufzeitErgebnisStdev() {
		return laufzeitErgebnisStdev;
	}

	public void setLaufzeitErgebnisStdev(double laufzeitErgebnisStdev) {
		this.laufzeitErgebnisStdev = laufzeitErgebnisStdev;
	}

	public double getLaufzeitErgebnisMedian() {
		return laufzeitErgebnisMedian;
	}

	public void setLaufzeitErgebnisMedian(double laufzeitErgebnisMedian) {
		this.laufzeitErgebnisMedian = laufzeitErgebnisMedian;
	}

	public long getLaufzeitErgebnisMin() {
		return laufzeitErgebnisMin;
	}

	public void setLaufzeitErgebnisMin(long laufzeitErgebnisMin) {
		this.laufzeitErgebnisMin = laufzeitErgebnisMin;
	}

	public long getLaufzeitErgebnisMax() {
		return laufzeitErgebnisMax;
	}

	public void setLaufzeitErgebnisMax(long laufzeitErgebnisMax) {
		this.laufzeitErgebnisMax = laufzeitErgebnisMax;
	}

	public static ArrayList<Long> getLaufzeitErgebnisse() {
		return laufzeitErgebnisse;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public int getLoopCounter() {
		return loopCounter;
	}

	public void setLoopCounter(int loopCounter) {
		this.loopCounter = loopCounter;
	}

	public double getAnzahlLoopsPerMinute() {
		return anzahlLoopsPerMinute;
	}

	public void setAnzahlLoopsPerMinute(double anzahlLoopsPerMinute) {
		this.anzahlLoopsPerMinute = anzahlLoopsPerMinute;
	}

  public void setPreprocessingRunTime(long preprocessingRunTime) {
    this.preprocessingRunTime = preprocessingRunTime;
  }

  public void setClassificationRunTime(long classificationRunTime) {
    this.classificationRunTime = classificationRunTime;
  }

  public void printCurrentGateRunTimes() {
    workerThreadLogger.info("Current gate run-times per Document:");
    workerThreadLogger.info("Preprocessing RunTime: " + preprocessingRunTime);
    workerThreadLogger.info("Classification RunTime: " + classificationRunTime );
  }

  public void addWriteFileTime(String name, long runTime) {
    writeFileTimes.put(name,runTime);    
  }
  
  public static HashMap<String, Long> getWriteFileTimes() {
    return writeFileTimes;
  }

  public static void setWriteFileTimes(HashMap<String, Long> writeFileTimes) {
    PerformanceMeasurement.writeFileTimes = writeFileTimes;
  }

  public void printFileWritingTimes() {
    
    Iterator it = writeFileTimes.entrySet().iterator();
    while(it.hasNext()){
      Map.Entry pair = (Map.Entry) it.next();
      workerThreadLogger.info("writingFileRunTime: \t" + pair.getKey() + "\t" + pair.getValue());
    }
    
  }
}
