package gov.nasa.gibs.mrfgen.server;

import gov.nasa.gibs.generate.api.*;
import gov.nasa.gibs.generate.api.engine.*;
//import gov.nasa.horizon.common.util.*;
import gov.nasa.horizon.common.api.zookeeper.api.constants.RegistrationStatus;
import gov.nasa.horizon.common.api.zookeeper.api.ZkAccess;
import gov.nasa.horizon.common.api.zookeeper.api.ZkFactory;
import gov.nasa.horizon.sigevent.api.SigEvent;
import gov.nasa.horizon.sigevent.api.EventType;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//TODO Remove this configurator
import org.apache.log4j.BasicConfigurator;
import org.apache.zookeeper.*;
import org.apache.commons.configuration.*;
import org.apache.commons.cli.*;

//import gov.nasa.horizon.common.api.serviceprofile.*;

/**
 * @author calarcon
 * 
 */
public class MrfGenServer implements Watcher{

   private ZkAccess zk;
   //Domain domain
   //Keychain keychain;
   String domainFile = System.getProperty(Constants.PROP_DOMAIN_FILE);

   String federation;

   String serverName;
   String registryUrl;
   String zooKeeperUrl;
   //Stereotype stereotype;
   String storageName;
   Integer maxWaitTime;
   boolean started = false;
   String sigEventUrl;
   String outputDir;
   Integer threadMax;
   
   String testJson;

   long serverId;
   long serverSessionId;
   String sessionToken;

   private final Integer MAX_JOBS = 20;
   private String LOCAL_HOST_NAME = "";//InetAddress.getLocalHost().getHostName();
   private String SIG_EVENT_CATEGORY = "MRFGEN";

   static Log log = LogFactory.getLog(MrfGenServer.class);

   private static final MrfGenServer INSTANCE = new MrfGenServer();

   static MrfGenServer getInstance() {
      return INSTANCE;
   }

   public MrfGenServer() {
	   try {
		   this.LOCAL_HOST_NAME = InetAddress.getLocalHost().getHostName();
	   }
	   catch (UnknownHostException e) {
		   this.LOCAL_HOST_NAME = "sigevent";
	   }
   }

   /*
    * private void deleteFiles(files) { files.each { File f = new File(it) if (f.exists()) { f.delete() } } }
    */

   void activate(String[] args) throws IOException {
      /*
       * addShutdownHook() { try { if (ServerMonitor.instance.serverInShutdown) { int remaingTrans = ServerMonitor.instance.startShutdown(0) log.warn "Server ${serverName} terminated ${remaingTrans} transactions."
       * 
       * // TODO add sigevent dispatch code here }
       * 
       * if (started) { // shutdown notification to sigevent only if engine has been started sendSigEvent( EventType.Info, "MRF Generator Engine (${serverName}) has been shutdown.", "MRF Generator Engine (${serverName}) has been shutdown." ) } } catch (InterruptedException e) { log.warn(e) } }
       */
      //Log log = this.log;
      Thread.currentThread().setName("MRFGEN");

      log.info(Constants.COPYRIGHT);
      log.info(Constants.SERVER_VERSION_STR + "\n");

      parseArgs(args);
      configure();

      //Final check of required params
      validateInput();

      startZooKeeperConnection();
   }

   private void parseArgs(String[] args) {
      CommandLineParser clp = new BasicParser();

      Options options = new Options();
      options.addOption("f", "federation", true, "Federation Name");
      options.addOption("n", "name", true, "MRF Generation Engine Name");
      options.addOption("w", "wait", true, "Maximum engine wait time in milliseconds");
      options.addOption("m", "maxthreads", true, "Maximum number of MRF Generation threads to spawn a time.");
      options.addOption("u", "username", true, "User name");
      options.addOption("p", "password", true, "Password");
      options.addOption("r", "registry", true, "Registry URL");
      options.addOption("e", "sigevent", true, "Sigevent URL");
      options.addOption("z", "zookeeper", true, "Zookeeper URL");
      options.addOption("o", "output", true, "Output directory for configuration files. If none specified, defaults to output directory of the generated MRF file.");
      options.addOption("h", "help", false, "Print usage");
      options.addOption("t", "test", true, "Test json input instead of ZK query");

      String app = System.getProperty(Constants.PROP_USER_APP);//TODO PROP_USER_APP);
      if (app == null) {
         app = MrfGenServer.class.getName();
      }

      HelpFormatter helpFormatter = new HelpFormatter();
      try {
         CommandLine cl = clp.parse(options, args);
         if (cl.hasOption("help")) {
            helpFormatter.printHelp("mrfgenserver", options);
            System.exit(0);
         }


         federation = cl.getOptionValue("federation");
         serverName = cl.getOptionValue("name");
         zooKeeperUrl = cl.getOptionValue("zookeeper");
         if (cl.hasOption("wait")) {
            maxWaitTime = Integer.parseInt(cl.getOptionValue("wait"));
         }
         if(cl.hasOption("maxthreads")) {
            threadMax = Integer.parseInt(cl.getOptionValue("maxthreads"));
         }
         outputDir = cl.getOptionValue("output");
         sigEventUrl = cl.getOptionValue("sigevent");
         
         testJson = cl.getOptionValue("test");
         
      } catch (Exception e) {
         log.error(e.getMessage());
         helpFormatter.printHelp("mrfgenserver", options);
         System.exit(0);
      }

   }

   private void configure() {
      if (federation == null || sigEventUrl == null || zooKeeperUrl == null || serverName == null) {
         ServerDomain domain = new ServerDomain();
         log.debug("Domain file specified: "+domainFile);
         if (domainFile != null && !domainFile.isEmpty()) {
            try {
               domain.load(domainFile);
               if (federation == null)
                  federation = domain.getDefault();
               if (registryUrl == null)
                  registryUrl = domain.getUrl(federation);
               if (sigEventUrl == null)
                  sigEventUrl = domain.getSigEvent();
               if (zooKeeperUrl == null)
                  zooKeeperUrl = domain.getJobKeeper();
               if (outputDir == null)
                  outputDir = domain.getOutputDir();
               if (serverName == null)
                  serverName = domain.getServerName();
               if (outputDir == null)
                  outputDir = domain.getOutputDir();
               if (maxWaitTime == null)
                  maxWaitTime = domain.getMaxWaitTime(1000);
               if (threadMax == null)
                  threadMax = domain.getThreadMax(1);
               System.setProperty("generate.mrfgen.path", domain.getMrfGenPath());
            } catch (ConfigurationException e) {
               log.warn("No configuration file found.");
            }
         } 
      }
      else {
         //Set param defaults if the minimum params have been set
         if(maxWaitTime == null)
            maxWaitTime = 1000;
         if(threadMax == null)
            threadMax = 10;
      }
   }
   
   private void validateInput() {

      if(federation == null || sigEventUrl == null || zooKeeperUrl == null || serverName == null ) {
         StringBuilder missingParams = new StringBuilder();
         if(federation == null)
            missingParams.append("federation ");
         if(sigEventUrl == null)
            missingParams.append("sigEventUrl ");
         if(zooKeeperUrl == null)
            missingParams.append("zooKeeperUrl ");
         if(serverName == null)
            missingParams.append("serverName ");
         log.error("Insufficient params in command line or config file: "+missingParams.toString());
         System.exit(0);
      }
   }

   private void sendSigEvent(EventType eventType, String description, String data) {
      //TODO remove comment
      SigEvent sigEvent = new SigEvent(sigEventUrl);
      sigEvent.create(eventType, this.SIG_EVENT_CATEGORY, "mrfgenerator", "mrfgenerator", this.LOCAL_HOST_NAME, description, null, data);
      log.info("SigEvent: " + description);
   }

   void startZooKeeperConnection() {
      Engine engineData = EngineFactory.createProfile(this.MAX_JOBS);
      log.info("Connecting to Zookeeper service at: "+zooKeeperUrl);
      try {
         zk = (ZkAccess)ZkFactory.getZk(zooKeeperUrl, null, this);
         Object initialStatus = zk.checkGeneratorRegistration(federation, serverName);
         //if(initialStatus != RegistrationStatus.READY) {
            engineData.setName(serverName);
            engineData.setFederation(federation);
            engineData.setStarted(new Date());
            engineData.setStatus(RegistrationStatus.READY.toString());
            String path = zk.registerGenerator(federation, serverName, engineData.toString());
            if (path == null) {
               log.error("Unable to register engine " + serverName + " with zookeeper for federation " + federation);
               return;
            }
        // }
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         return;
      }
      started = true;
      log.info("Connection successful!");
      sendSigEvent(EventType.Info, "MRF Generation engine (" + serverName + ") registered with federation (" + federation + ") has been started.", "MRF Generation engine (" + serverName + ") registered with federation (" + federation + ") has been started.");
      new Thread(new ZkLoopThread(zk, federation, serverName, sigEventUrl, threadMax, maxWaitTime, engineData, testJson), "MRFGEN-ZKLoop").start();
   }

   //Watcher implementation of process()
   public void process(WatchedEvent e) {
      //Do nothing (mostly to avoid an error message)
   }

   public static void main(String[] args) throws Exception {
      //TODO get rid of this configurator
      //BasicConfigurator.configure();
     MrfGenServer.getInstance().activate(args);
   }
   

}
