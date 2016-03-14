package gov.nasa.gibs.generate.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.configuration.*;
import java.util.*;
import java.io.*;

public class ServerDomain  {
   private static Log log = LogFactory.getLog(ServerDomain.class);
   private XMLConfiguration config;
   //Constructors
   public ServerDomain() { }
   public ServerDomain(String domainFilePath) {
      try {
         load(domainFilePath);
      }
      catch (ConfigurationException e) {
         log.error("Could not load configuration file: "+domainFilePath);
      }
   }

   public void load(String domainFilePath) throws ConfigurationException {
      File domainFile = new File(domainFilePath);
      config = new XMLConfiguration(domainFile);
   }

   public String getDefault() {
      return config.getString("default");
   }

   public String getUrl(String federation) {
      List<HierarchicalConfiguration> federations = config.configurationsAt("domain.federation");
      for(HierarchicalConfiguration fed: federations) {
         if(fed.getString("name").equals(federation)) {
            return fed.getString("url");
         }
      }
      return null;
   }
   public String getSigEvent() {
      return config.getString("sigevent");
   }
   
   public String getJobKeeper() {
      return config.getString("jobkeeper.server");
   }
   
   public String getJobKeeperWebService() {
      return config.getString("jobkeeper.webservice");
   }
   
   public String getInventory() {
      return config.getString("inventory");
   }
   
   public String getSecurity() {
      return config.getString("security");
   }
   
   public String getOutputDir() {
      return config.getString("configOutputDir");
   }
   
   public String getServerName() {
      return config.getString("serverName");
   }
   
   public Integer getMaxWaitTime(Integer defaultWait) {
      return config.getInteger("maxWaitTime", defaultWait);
   }
   
   public Integer getThreadMax(Integer defaultMax) {
      return config.getInteger("threadMax", defaultMax);
   }
   
   public String getMrfGenPath() {
      return config.getString("mrfgenPath");
   }
}