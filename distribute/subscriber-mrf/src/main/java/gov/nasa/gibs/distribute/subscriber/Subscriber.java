package gov.nasa.gibs.distribute.subscriber;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.*;
import org.apache.commons.io.*;
import org.apache.commons.configuration.*;

import gov.nasa.horizon.inventory.model.*;
import gov.nasa.horizon.inventory.api.*;
import gov.nasa.gibs.distribute.subscriber.api.*;
import gov.nasa.gibs.distribute.subscriber.linkgen.LinkGen;
import gov.nasa.gibs.distribute.subscriber.plugins.MrfScript;
import gov.nasa.horizon.sigevent.api.SigEvent;
import gov.nasa.horizon.sigevent.api.Response;
import gov.nasa.horizon.sigevent.api.EventType;
import gov.nasa.horizon.common.api.zookeeper.api.constants.RegistrationStatus;
import gov.nasa.horizon.common.api.zookeeper.api.*;

import org.apache.zookeeper.*;

public class Subscriber implements Watcher {
   private final String COPYRIGHT = "Copyright 2014, Jet Propulsion Laboratory, Caltech, NASA";
   private final String VERSION = "0.4.0";
   private final String VERSION_DATE = "March 2014";
   private final String VERSION_SUMMARY = "MRF Subscriber Package "+VERSION+", "+VERSION_DATE ;

   private gov.nasa.gibs.distribute.subscriber.api.DataSubscriber dataSubscriber;
   private String dataDir;
   private String outputBasePath;
   private String configPath, lookupPath, linkPath;
   private XMLConfiguration mapping;
   
   private String productTypeNames;
   private int sleepTime = 0;
   //private String usage = "";
   private Options m_options;
   private String[] m_args;
   private HashSet<ProductType> listOfProductTypes = new HashSet<ProductType>();

   private ZkAccess zk;

   //Sig Event items
   static String m_sigEventUrl = null;
   static SigEvent m_sigEventObject = null; // Used for reporting significant events.
   private String m_executionErrorCategory;
   private List<String> m_sigMessages = new ArrayList<String>();

   //Configuration holders
   private String subscriberClassName;
   private String target;
   static Log _logger = LogFactory.getLog(Subscriber.class);

   private boolean daemonFlag;
   private boolean linkFlag = false;
   //Batch mode properties
   private Date sTime, eTime;
   //Link mode properties
   private XMLConfiguration linkMapping;

   public Subscriber(String[] args) {
      this.m_args = args;
      //empty constructor for now
   }

   public static void main(String[] args) {
      new Subscriber(args).start();
   }

   protected void start() {
      _logger.info(COPYRIGHT);
      _logger.info(VERSION_SUMMARY);

      //Beginning of subscription program
      this.configPath = System.getProperty("distribute.config.file", "../conf/distribute.config");
      this.lookupPath = System.getProperty("distribute.source.lookup", "../conf/mrf_config.xml");
      this.linkPath = System.getProperty("link.config.file", "../conf/link_config.xml");
      _createOptions();
      _loadCmdLineOpts();
      _configure();
      if (!_hasRequiredOptions()) {
         _printUsage();
         System.exit(-1);
      }
      
      _logger.info("Starting subscription at " + new Date().toString());
      _registerSigEvent();
      
      if(!linkFlag) {
         _initZk();
      }
      else {
         _logger.info("*** LINK MODE: Managing symbolic links with no connection to ZK");
      }
      _createCacheDirectory();

      //_createProductTypeDirectories();

      dataSubscriber = _setUpSubscriber();

      //main program area
      //setLastRunTime();
      do {
          //Reload configuration each run
          _configure();
          _createProductTypesFromNames();
         //setLastRunTime();
         _logger.info("Beginning subscription request.");
         for (ProductType pt : listOfProductTypes) {
            // Start out with an empty list of sigevent messages for every iteration.
            if (this.m_sigMessages != null) {
               this.m_sigMessages.clear();
            }
            
            List<Product> foundProducts = null; 
            File cacheFile = null;
            if(daemonFlag) {
               String filePath;
               if(linkFlag) {
                  filePath = dataDir + File.separator + pt.getIdentifier() + "-" + "link-cacheFile.txt";
               }
               else {
                  filePath = dataDir + File.separator + pt.getIdentifier() + "-" + "cacheFile.txt";
               }
               cacheFile = new File(filePath);
               Date ptLastRunTime = getLastRunTime(cacheFile);
               
               //Using cache file value (default to midnight today if none) 
               foundProducts = dataSubscriber.list(pt, ptLastRunTime);
            }
            else {
               //Use command line params to form time range search (push start and end times to extremes)
               foundProducts = dataSubscriber.listRange(pt, getStartOfDay(this.sTime), getEndOfDay(this.eTime));
            }
            
            if(foundProducts == null || foundProducts.isEmpty()) {
               _logger.info("No products found for ProductType: "+pt.getIdentifier());
            }
            else if (foundProducts != null && !foundProducts.isEmpty()) {
               _logger.info("Found "+foundProducts.size()+" products for ProductType:" + pt.getIdentifier());
               try {

                  _logger.info("Running external script on "+foundProducts.size()+" products found for " + pt.getIdentifier());
                  if(linkFlag) {
                     LinkGen.processLinks(foundProducts, pt.getIdentifier(), linkMapping, daemonFlag, target);
                  }
                     
                  else {
                     MrfScript.processProducts(zk, foundProducts, pt.getIdentifier(), mapping, daemonFlag, target);
                  }

                  // If daemon mode, update the cache file
                  if(daemonFlag) {
                     _logger.info("Writing timestamp cache for current Product Type: " + pt.getIdentifier());
                     Date newLastRunTime = _getLastArchiveTime(foundProducts);
                     try {
                        FileUtils.writeStringToFile(cacheFile, String.valueOf(newLastRunTime.getTime()+1));
                     } catch (IOException e) {
                        _logger.error("Could not create cache file: " + cacheFile.getAbsoluteFile());
                     }
                  }
               } catch (Exception e) {
                  _logger.warn(e.getClass().toString(), e);
               }

            }
            //post productType specific sig events
            _postSigMessages(pt.getIdentifier());
         }

         if (sleepTime != 0 && daemonFlag) {
            try {
               _logger.info("Sleeping for " + sleepTime / 1000 + " seconds.");
               Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }
         else if(!daemonFlag){
            _logger.info("*** BATCH MODE ENDED");
         }
      } while (daemonFlag);
      //end main program area

   }

   private void _registerSigEvent() {

      if (m_sigEventUrl == null) {
         _logger.error("Field sigevent.url is missing in config file");
         System.exit(0);
      }

      // Since the URL is valid, we also create one instance of SigEvent object to be used through out the life of the program.

      try {
         m_sigEventObject = new SigEvent(m_sigEventUrl);
      }
      catch (Exception e) {
         _logger.warn("Could not start connection to Sigevent... no messages will be posted");
      }

      if (m_sigEventObject == null) {
         _logger.error("Cannot create SigEvent object from m_sigEventUrl [" + m_sigEventUrl + "].  Program exiting.");
         System.exit(0);
      }
   }

   private void _createCacheDirectory() {
      outputBasePath = this.outputBasePath;
      if (!outputBasePath.endsWith(File.separator))
         outputBasePath = outputBasePath + File.separator;

      this.dataDir = outputBasePath;

      File f_dataDir = new File(dataDir);

      if (!f_dataDir.exists()) {
         if (f_dataDir.mkdirs())
            _logger.info("Created data directory at '" + dataDir + "'");
         else {
            _logger.error("Could not create data directory '" + dataDir + "'; Check permissions and run the subscriber again.");
            System.exit(5);
         }
      }
   }

   private void _postSigMessages(String productTypeName) {
      StringBuilder sb = new StringBuilder();
      for (String s : m_sigMessages) {
         sb.append(s);
         sb.append('\n');
      }
      m_sigMessages = new ArrayList<String>();
      if (sb.toString().equals(""))
         _logger.debug("No messages to send.");
      else
         _postSig(EventType.Error, "Subscriber-MRF", "Subscriber error report [productType:" + productTypeName + " ]", sb.toString(), productTypeName);

   }

   private void _postSig(String message) {
      m_sigMessages.add(message);
   }

   private static void _postSig(EventType i_eventType, String i_category, String i_description, String i_data, String source) {

      source = "DISTRIBUTE-SUBSCRIBER"; // Reset to see if the error will go away.  It does.  A note about this field: There may be an upper limit of about 20 characters or less.

      String provider = "JPL"; // Dummy provider.
      String computer = null; // Dummy computer.
      Integer pid = new Integer(0); // Dummy process id.

      try {
         InetAddress addr = InetAddress.getLocalHost();
         byte[] ipAddr = addr.getAddress();
         String hostname = addr.getHostName();
         computer = hostname;

      } catch (UnknownHostException e) {

      }

      // Regardless of the category, we post the significant event to "ALL" category.
      Response sigResponse = m_sigEventObject.create(i_eventType, i_category, source, provider, computer, i_description, null, i_data);

      // Just log to error if we cannot report significant event.  Re-submit if category is not "ALL".
      if (sigResponse.hasError()) {
         //_logger.error("Cannot report SigEvent due these reason(s): key = value ");
         _logger.error("Cannot report SigEvent due to: " + sigResponse.getError().toString());
      }
   }

   private void _createProductTypesFromNames() {

      listOfProductTypes = new HashSet<ProductType>();
      if(productTypeNames == null) {
          List<HierarchicalConfiguration> productTypes = mapping.configurationsAt("productTypes.productType");
          for(HierarchicalConfiguration productType:productTypes) {
              String ptName = productType.getString("name");
              ProductType pt = new ProductType(ptName);
              listOfProductTypes.add(pt);
          }
      }
      else {
          for (String name : productTypeNames.split(",")) {
             ProductType pt = new ProductType(name);
             listOfProductTypes.add(pt);
          }
      }
   }

   private Date getLastRunTime(File cacheFile) {
      TimeZone gmt = TimeZone.getTimeZone("GMT");
      GregorianCalendar todayMidnight = new GregorianCalendar(gmt);
      todayMidnight.set(Calendar.HOUR_OF_DAY, 0);
      todayMidnight.set(Calendar.MINUTE, 0);
      todayMidnight.set(Calendar.SECOND, 0);
      todayMidnight.set(Calendar.MILLISECOND, 0);
      return getLastRunTime(cacheFile, todayMidnight.getTime());
   }

   private Date getLastRunTime(File cacheFile, Date defaultDate) {
      //Set default return value to midnight today GMT 
      Date lastRunTime = new Date();

      lastRunTime = defaultDate;

      String timeString = null;
      try {
         timeString = FileUtils.readFileToString(cacheFile);
      } catch (IOException e) {
         _logger.warn("Could not find or open cache file: " + cacheFile.toString() + " ... defaulting to midnight today");
      }
      if (timeString != null) {
         lastRunTime = new Date(Long.valueOf(timeString));
      }
      return lastRunTime;
   }

   private gov.nasa.gibs.distribute.subscriber.api.DataSubscriber _setUpSubscriber() {

      ClassLoader clazzLoader;
      Class clazz;
      clazzLoader = this.getClass().getClassLoader();
      try {
         clazz = clazzLoader.loadClass(subscriberClassName);
         return (gov.nasa.gibs.distribute.subscriber.api.DataSubscriber) clazz.newInstance();

      } catch (ClassNotFoundException e) {
         e.printStackTrace();
         System.exit(-10);
      } catch (InstantiationException e) {
         e.printStackTrace();
         System.exit(-10);
      } catch (IllegalAccessException e) {
         e.printStackTrace();
         System.exit(-10);
      }

      return null;
   }
   
   private void _createOptions() {
      m_options = new Options();

      // Add the possible options:
      m_options.addOption("p", "producttype", true, "The name of the productType you wish to subscribe to, or a coma delimited set of productTypes.");
      m_options.addOption("t", "target", true, "The target to use in the configuration mapping (NOT a product type name).");
      m_options.addOption("l", "link", true, "Specify a link configuration file and proceed with link generation/management");
      m_options.addOption("s", "start", true, "The start time for the crawler to use. Defaults to the current time if not specified. Format: yyyy-MM-dd");
      m_options.addOption("e", "end", true, "The end time for the crawler to use. Defaults to the current time if not specified. Format: yyyy-MM-dd");
      m_options.addOption("n", "name", true, "The name for the subscriber instance. Will be used for log file naming.");
      //m_options.addOption("c", "config", true, "Path the the subscriber configuration file (defaults to ../conf/distribute.config)");
      m_options.addOption("m", "mrf", true, "Path the the subscriber source to mrf lookup file (defaults to ../conf/mrf_config.xml)");
      m_options.addOption("h", "help", false, "Print usage");
   }

   private void _loadCmdLineOpts() {

      CommandLineParser clp = new BasicParser();
      try {
         CommandLine cl = clp.parse(m_options, m_args);
         if (cl.hasOption("help")) {
            _printUsage();
            System.exit(0);
         }
         //if (cl.hasOption("config")) {
         //   this.configPath = cl.getOptionValue("config");
         //}
         if(cl.hasOption("mrf")) {
            this.lookupPath = cl.getOptionValue("mrf");
         }

         if (cl.hasOption("producttype")) {
            this.productTypeNames = cl.getOptionValue("producttype");
         }
         if(cl.hasOption("target")) {
            this.target = cl.getOptionValue("target");
         }
         if(cl.hasOption("link")) {
            this.linkFlag = true;
            this.linkPath = cl.getOptionValue("link");
         }
         if (cl.hasOption("start")) {
            String startTimeString = cl.getOptionValue("start");
            this.sTime = this._createDateFromString(startTimeString);
         }
         if (cl.hasOption("end")) {
            String startTimeString = cl.getOptionValue("end");
            this.eTime = this._createDateFromString(startTimeString);
         }

      } catch (ParseException e) {
         _logger.error("Unable to parse command line options: " + e.getMessage());
         _printUsage();
         System.exit(0);
      }
   }
   
   private void _configure() {

      // Load external files and fill in values
      if (configPath != null) {
         try {
            PropertiesConfiguration config = new PropertiesConfiguration(configPath);
            outputBasePath = config.getString("distribute.cache.path");
            if(outputBasePath == null || outputBasePath.equals("")) {
               outputBasePath = System.getProperty("user.home")+File.separator+".horizon"+File.separator+"subscriber-cache";
            }
            sleepTime = 1000 * Integer.parseInt(config.getString("distribute.sleep.seconds"));
            m_sigEventUrl = config.getString("sigevent.url");
            System.setProperty("inventory.url", config.getString("inventory.url"));
            System.setProperty("zookeeper.url", config.getString("zookeeper.url"));
            System.setProperty("distribute.mrf.output.path", config.getString("distribute.mrf.output.path"));
            System.setProperty("distribute.mrf.extrafiles.path", config.getString("distribute.mrf.extrafiles.path"));
            subscriberClassName = config.getString("distribute.subscriber.class");
         } catch (ConfigurationException e) {
            _logger.error("Could not load config file: " + configPath);
            System.exit(0);
         }

      }
      if(!linkFlag && lookupPath != null) {
         try {
            this.mapping = new XMLConfiguration(lookupPath);
         }
         catch(ConfigurationException e){
            _logger.error("Could not load source to product type mapping file: "+lookupPath);
            System.exit(0);
         }
      }
      
      if(linkFlag && linkPath != null) {
         try {
            this.linkMapping = new XMLConfiguration(linkPath);
         }
         catch(ConfigurationException e) {
            _logger.error("Could not load link configuration file: "+linkPath);
            System.exit(0);
         }
      }
      
      //Process Daemon or Batch mode
      if(this.sTime == null && this.eTime == null) {
         this.daemonFlag = true;
         _logger.info("*** DAEMON MODE: No start or end time specified.");
      }
      else {
         this.daemonFlag = false;
         if(this.sTime == null) {
            this.sTime = new Date(0);
            _logger.info("*** BATCH MODE: Defaulting start to Jan 1, 1970. (End time: "+this.eTime+")");
         }
         else if(this.eTime == null) {
            this.eTime = new Date();
            _logger.info("*** BATCH MODE: Default end to current time. (Start time: "+this.sTime+")");
         }
         else {
            _logger.info("*** BATCH MODE: Processing jobs from "+this.sTime+" to "+this.eTime);
         }
      }
      
   }

   private boolean _hasRequiredOptions() {
      //String[] requiredOptions = new String[] {"d", "e"};
      boolean result = true;

      if (outputBasePath == null) {
         _logger.error("Cache path is not set. Cannot run subscriber in daemon mode.");
         result = false;
      }

//      if (productTypeNames == null) {
//         _logger.error("In order to subscribe to a productType, a productType name must be provided with the -p <name> option.");
//         result = false;
//      }
      if(mapping == null && linkMapping == null) {
         _logger.error("Mapping file not valid. Please update launcher script or specify on launch with '-m <PATH_TO_MAPPING_FILE>' OR '-l <PATH_TO_LINK_CONFIG>'");
         result = false;
      }
      return result;
   }

   private Date _createDateFromString(String i_date) {

      Date date = null;
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      try {
         date = formatter.parse(i_date);
      } catch (java.text.ParseException e) {
         _logger.error("Could not parse date from string: " + i_date);
         _logger.error("Exiting...");
         e.printStackTrace();
         System.exit(5);
      }
      return date;
   }

   private void _initZk() {
      
      try {
         String zooKeeperUrl = System.getProperty("zookeeper.url");
         _logger.info("Attempting connection to ZooKeeper at: "+zooKeeperUrl);
         zk = (ZkAccess) ZkFactory.getZk(zooKeeperUrl, null, this);
      } catch (Exception e) {
         _logger.error(e.getMessage(), e);
         System.exit(0);
      }
      _logger.info("Zookeeper connected!");
   }

   private Date _getLastArchiveTime(List<Product> products) {
      Date latestArchive = new Date(0);
      for (Product p : products) {
         if (p.getArchiveTime().after(latestArchive)) {
            latestArchive = p.getArchiveTime();
         }
      }
      return latestArchive;
   }
   
   //Utilities (Static for reuse outside of class)
   
   public static Date getStartOfDay(Date date) {
      return DateUtils.truncate(date, Calendar.DATE);
  }
   public static Date getEndOfDay(Date date) {
      return DateUtils.addMilliseconds(DateUtils.ceiling(date, Calendar.DATE), -1);
  }

   private void _printUsage() {
      String _userScript = new String("mrfsubscriber");
      String USAGE_HEADER = new String("MRF Inventory Subscriber");
      HelpFormatter formatter = new HelpFormatter();

      formatter.printHelp(_userScript, USAGE_HEADER, m_options, null, true);
   }
   
   //Method to satisfy watcher implementation
   public void process(WatchedEvent we) {
   }
   
   

}
