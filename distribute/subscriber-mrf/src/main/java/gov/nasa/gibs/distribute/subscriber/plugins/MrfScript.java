package gov.nasa.gibs.distribute.subscriber.plugins;

import java.util.*;
import java.io.*;
import java.text.*;

import gov.nasa.horizon.inventory.api.*;
import gov.nasa.horizon.inventory.model.Product;
import gov.nasa.horizon.inventory.model.ProductType;
import gov.nasa.horizon.inventory.model.ProductTypeGeneration;
import gov.nasa.horizon.inventory.model.ProductTypePolicy;
import gov.nasa.horizon.inventory.model.Source;
import gov.nasa.horizon.common.api.serviceprofile.SPSourceProduct;
import gov.nasa.horizon.common.api.zookeeper.api.constants.RegistrationStatus;
import gov.nasa.horizon.common.api.zookeeper.api.ZkAccess;
import gov.nasa.horizon.common.api.zookeeper.api.ZkFactory;
import gov.nasa.gibs.distribute.subscriber.Subscriber;
import gov.nasa.gibs.generate.api.job.*;
import groovyx.net.http.Status;

import org.apache.commons.logging.*;
import org.apache.commons.lang.time.*;
import org.apache.commons.configuration.*;
import org.apache.commons.lang.*;



public class MrfScript {
   
   static Log log = LogFactory.getLog(MrfScript.class);

   public static void processProducts(ZkAccess zk, List<Product> products, String productTypeName, XMLConfiguration mapping, boolean daemonFlag, String target) throws Exception{
      
      //Initiating inventory connection
      InventoryApi q = new InventoryApi(System.getProperty("inventory.url"));
      
      String parameter = null;
      try {
         parameter = getTargetProductType(productTypeName, mapping, target);
      }
      catch(ConfigurationException e) {
         log.error(e.getMessage(), e);
         return;
      }
      if(parameter == null) {
         log.error("No target Product Type found for "+productTypeName);
         return;
      }
      
      String imageType = null;
      String sourceEPSG = null;
      String targetEPSG = null;
      String colormap = null;
      String emptyTile = null;
      
      Integer outputSizeX = null;
      Integer outputSizeY = null;
      Integer overviewScale = null;
      Integer overviewLevels = null;
      String overviewResample = null;
      String resizeResample = null;
      String reprojectionResample = null;
      String vrtNodata = null;
      Integer mrfBlockSize = null;
      String extents;
      String targetExtents;
      Boolean subDaily = false;
      
      Set<Date> datesForProcess = new HashSet<Date>();
      
      try {
         imageType = q.getProductTypePolicy(parameter).getDataFormat();
         sourceEPSG = q.getEPSG(productTypeName);
         targetEPSG = q.getEPSG(parameter);
         emptyTile = q.getEmptyTile(parameter);
         extents = q.getExtents(productTypeName);
         targetExtents = q.getExtents(parameter);
         if (imageType.endsWith("PPNG")) {
            colormap = q.getColormap(parameter);
         }
         if(imageType.contains("SUBDAILY")) {
             subDaily = true;
         }
         
         //Generation table stuff
         ProductTypeGeneration gen = q.getGeneration(parameter);
         if(gen != null) {
            log.debug("Generation element found for ProductType "+parameter+"... adding to job config");
            outputSizeX = gen.getOutputSizeX();
            outputSizeY = gen.getOutputSizeY();
            overviewScale = gen.getOverviewScale();
            overviewLevels = gen.getOverviewLevels();
            overviewResample = gen.getOverviewResample();
            resizeResample = gen.getResizeResample();
            reprojectionResample = gen.getReprojectionResample();
            vrtNodata = gen.getVrtNodata();
            mrfBlockSize = gen.getMrfBlockSize();
         }
         else {
            log.debug("No generation element found for ProductType "+parameter);
         }
         for(Product p:products){
            //Add to the unique list of dates
            log.info("Found product: "+p.getName());
            if(subDaily) {
                datesForProcess.add(p.getStartTime());
            }
            else {
                datesForProcess.add(Subscriber.getStartOfDay(p.getStartTime()));
            }
         }
      }
      catch(InventoryException e) {
         log.error("Error communicating with the Inventory WS: getImageList or getSourceList");
         return;
      }
      
      
      for(Date startTime: datesForProcess) {
         log.info("ProductDate: "+startTime.toString());
         Date stopTime, stopTimeQuery;
         if(subDaily) {
             stopTimeQuery = startTime;
         }
         else {
             stopTimeQuery = Subscriber.getEndOfDay(startTime);
         }
         
         List<String> imageList = null;
         List<Source> sourceList = null;
         List<Product> productList = null;
         try {
            productList = q.getProductIdListAll(productTypeName, startTime, stopTimeQuery, null, true);
            imageList = q.getImageList(productTypeName, startTime, stopTimeQuery);
            log.debug("Ran getImageList with start time "+startTime.toString()+" and stop time "+stopTimeQuery.toString());
            //log.debug(imageList);
            sourceList  = q.getSourceList(productTypeName, startTime, stopTimeQuery);
            
            if(subDaily) {
                stopTime = productList.get(0).getStopTime();
            }
            else {
                stopTime = stopTimeQuery;
            }
         }
         catch(InventoryException e) {
            log.error("Error communicating with the Inventory WS: getImageList or getSourceList");
            return;
         }
         
         //Creating generate api object to post to ZK
         MrfGenProfile zkManifest = MrfGenProfileFactory.createProfile();
         zkManifest.setOperation("generate");
         
         List<MGJob> jobList = new ArrayList<MGJob>();
         MGJob job = zkManifest.createJob();
         
         for(Source sourceObj: sourceList ) {
            MGSource source = job.createSource();
            source.setProduct(sourceObj.getProduct());
            source.setProductType(sourceObj.getProductType());
            //source.setRepo(SPSourceProduct.SPMetadateaRepo.ECHO_OPENSEARCH);
            source.setRepo("ECHO_OPENSEARCH");
            job.addToSources(source);
         }
         
         Date now = new Date();
         String mrfProductName = parameter+"_"+new SimpleDateFormat("yyyyMMddHHmmss").format(startTime);
         String outputPath = System.getProperty("distribute.mrf.output.path")+File.separator+"staging"+File.separator+"data"+File.separator+parameter+File.separator+mrfProductName+"_"+now.getTime();
         String sipPath = System.getProperty("distribute.mrf.output.path")+File.separator+"staging"+File.separator+"pending"+File.separator+parameter;
         String workingPath;
         if(System.getProperty("distribute.mrf.extrafiles.path") != null) {
            workingPath = System.getProperty("distribute.mrf.extrafiles.path")+File.separator+parameter+File.separator+mrfProductName+File.separator+now.getTime();
         }
         else {
            workingPath = outputPath;
         }
         MGJobConfig config = job.createJobConfig();
         config.setOutputDir(outputPath);
         config.setWorkingDir(workingPath);
         if(imageType.startsWith("MRF-")) {
            config.setCompressionType(imageType.substring(4));
         }
         
         config.setDateOfData(startTime);
         config.setParameter(parameter);
         config.setMrfProductName(mrfProductName);
         config.setSipPath(sipPath);
         config.setInputs(imageList);
         if(colormap != null)
            config.setColormap(colormap);
         config.setMrfEmptyTileFilename(emptyTile);
         config.setExtents(extents);
         config.setTargetExtents(targetExtents);
         if(sourceEPSG != null && targetEPSG != null && sourceEPSG.length() > 5 && targetEPSG.length() > 5) {
            log.debug("EPSG codes found... Source: "+sourceEPSG+" Target: "+targetEPSG);
            config.setSourceEPSG(Integer.valueOf(sourceEPSG.substring(5)));
            config.setTargetEPSG(Integer.valueOf(targetEPSG.substring(5)));
         }
         
         //Generation fields
         config.setOutputSizeX(outputSizeX);
         config.setOutputSizeY(outputSizeY);
         config.setOverviewScale(overviewScale);
         config.setOverviewLevels(overviewLevels);
         config.setOverviewResample(overviewResample);
         config.setResizeResample(resizeResample);
         config.setReprojectionResample(reprojectionResample);
         config.setVrtNodata(vrtNodata);
         config.setStartTime(startTime.getTime());
         config.setStopTime(stopTime.getTime());

         
         //Setting default block size just in case it is not set.
         if(mrfBlockSize == null) {
            config.setMrfBlockSize(512);
         }
         else {
            config.setMrfBlockSize(mrfBlockSize);
         }
         
         //TODO remove these hardcode hacks
         //config.setVrt(0);
         //config.setBlockSize(512);
         //if(imageType.equals("JPG"))
         //   config.setEmptyTile("black");
         //else if(imageType.equals("PNG") || imageType.equals("PPNG"))
         //   config.setEmptyTile("transparent");
         
         config.setTimePosted(now);
         config.setDaemonFlag(daemonFlag);
         job.setJobConfig(config);
         jobList.add(job);
         zkManifest.setJobs(jobList);
         
         try {
            
            if(!daemonFlag) {
               //Running in batch mode... so just submit blindly
               zk.addToGenerationQueue("GIBS", zkManifest.toString(), null);
            }
            else {
               Date startOfToday = Subscriber.getStartOfDay(new Date());
               Calendar yesterday = Calendar.getInstance();
               yesterday.add(Calendar.DATE, -1);   
               Date startOfYesterday = Subscriber.getStartOfDay(yesterday.getTime());
               
               zk.addToGenerationQueue("GIBS", zkManifest.toString(), null);
               
/*               Date lastPosted = zk.getGenLastPosted(parameter);
               Date lastCompleted = zk.getGenLastCompleted(parameter);
               
               if (lastCompleted == null || lastPosted.before(lastCompleted)) {
                  //No running jobs
                  if(startOfYesterday.before(startTime) || startOfYesterday.equals(startTime)) {
                     //Product is NRT... post and update lastPosted timestamp
                     zk.addToGenerationQueue("GIBS", zkManifest.toString(), null);
                     zk.setGenLastPosted(parameter, now);
                  }
                  else {
                     //Product is standard... post 
                     zk.addToGenerationQueue("GIBS", zkManifest.toString(), null);
                  }
               }
               else {
                  //Job is currently in progress
                  if(startOfYesterday.before(startTime) || startOfYesterday.equals(startTime)) {
                     //Product is NRT... throw exception and wait until next subscriber iteration
                     throw new Exception("Job in progress for Product Type "+parameter+"... waiting until next iteration of subscriber");
                  }
                  else {
                     //Product is standard... post 
                     zk.addToGenerationQueue("GIBS", zkManifest.toString(), null);
                  }
               }*/
            }

            
            
            
            log.debug("Added job to generation queue");
         }
         catch(IOException e) {
            log.error("Could not add generation job to ZK Queue");
         }
         //System.out.println(zkManifest.toString());
      }
      
   }
   
   public static String getTargetProductType(String productTypeName, XMLConfiguration mapping, String target) throws ConfigurationException{
      String targetProductType = null;
      
      List<HierarchicalConfiguration> productTypes = mapping.configurationsAt("productTypes.productType");
      log.trace("ProductTypes count is "+ productTypes.size());
      for (HierarchicalConfiguration pt : productTypes) {
         if(pt.getString("name").equals(productTypeName)) {
            if(target != null) {
               List<HierarchicalConfiguration> targets = pt.configurationsAt("targets.target");
               for (HierarchicalConfiguration possibleTarget : targets) {
                  if(possibleTarget.getString("type").equals(target)) {
                     targetProductType = possibleTarget.getString("name");
                  }
               }
            }
            if (targetProductType == null){
               targetProductType = pt.getString("targets.default");
            }
         }
      }
      if(targetProductType == null) {
         throw new ConfigurationException("No target Product Type found for "+productTypeName);
      }
      else {
         return targetProductType;
      }
   }

}
