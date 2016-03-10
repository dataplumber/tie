package gov.nasa.gibs.mrfgen.server;

import gov.nasa.horizon.common.api.util.SystemProcess;
import gov.nasa.horizon.common.api.util.Errno;
import gov.nasa.horizon.common.api.zookeeper.api.*;
import gov.nasa.horizon.common.api.serviceprofile.*;
import gov.nasa.gibs.generate.api.engine.Engine;
import gov.nasa.gibs.generate.api.engine.EngineFactory;
import gov.nasa.gibs.generate.api.job.*;
import gov.nasa.horizon.sigevent.api.SigEvent;
import gov.nasa.horizon.sigevent.api.EventType;

import java.io.*;
import java.text.*;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.configuration.*;
import org.apache.commons.lang.*;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.io.*;
import org.apache.commons.codec.digest.*;

public class GenerationThread implements Runnable {
   private static Log log = LogFactory.getLog(GenerationThread.class);
   private ZkAccess zk;
   private MGJobConfig jobConfig;
   private List<MGSource> sources;
   private Semaphore threadBouncer;
   private String configDir;
   private String sigEventUrl;
   private Engine engineData;
   private Boolean passFlag;
   
   private String LOCAL_HOST_NAME = "hostname";//TODO InetAddress.getLocalHost().getHostName();
   private String SIG_EVENT_CATEGORY = "MRFGEN";

   public GenerationThread(ZkAccess zk, MGJobConfig jobConfig, List<MGSource> sources, String configDir, String sigevent, Semaphore threadBouncer, Engine engineData) {
      this.zk = zk;
      this.jobConfig = jobConfig;
      this.threadBouncer = threadBouncer;
      this.sources = sources;
      this.configDir = configDir;
      this.sigEventUrl = sigevent;
      this.engineData = engineData;
      this.passFlag = jobConfig.getDaemonFlag();
      
   }

   //Runnable interface implementation of run()
   public void run() {
      // Start an MRF Generation process
      Date genStartTime = new Date();
      this.jobConfig.setTimeStarted(genStartTime);
      
      Thread t = Thread.currentThread();
      String threadName = Thread.currentThread().getName();
      Path configPath;
      Path outputPath;
      Boolean subdailyFlag = false;

      log.info("Process started with name: " + threadName);
      
      // Update engineData with new job and post to ZK

      try {
         String newEngineDataString = zk.getGeneratorRegistration(engineData.getFederation(), engineData.getName());
         log.debug("Retrieving ZK Engine status before updating: "+newEngineDataString);
         Engine newEngineData = EngineFactory.createProfileFromMessage(newEngineDataString);
         if(newEngineData != null) {
            engineData = newEngineData;
         }
         engineData.addToCurrentJobs(jobConfig);
         zk.setGeneratorRegistration(engineData.getFederation(), engineData.getName(), engineData.toString());
         log.info("Updated engine node with new job: "+jobConfig.getMrfProductName());
      }
      catch(IOException e) {
         log.error("Could not update engine node: "+ e.getMessage());
      }

      //TODO Removing the generator rejection logic to test improved subscriber logic
      /*
      Date startOfToday = getStartOfDay(new Date());
      try {
         if(startOfToday.before(jobConfig.getDateOfData()) || startOfToday.equals(jobConfig.getDateOfData())) {
            Date lastPosted = zk.getGenLastPosted(jobConfig.getParameter());
            if (lastPosted != null && lastPosted.after(jobConfig.getTimePosted()) && !lastPosted.equals(jobConfig.getTimePosted()) && !passFlag) {
               log.warn("Skipping... job posting is earlier than most recently posted job in the same ProductType : " + jobConfig.getMrfProductName());
               //Remove job from engineData, fill in failure info then put into recent list.... then post to ZK
               processJobFailure("REJECTED: Job posting is earlier than most recently posted job in the same ProductType", null);
               threadBouncer.release();
               return;
            }
         }
      }
      catch(IOException e) {
         log.error("Error when performing rejection lastPosted check", e);
         processJobFailure("Error when performing LastPosted check", null);
         threadBouncer.release();
         return;
      }
      */
      outputPath = Paths.get(jobConfig.getOutputDir());
      try {
         Files.createDirectories(outputPath);
      }
      catch(IOException e) {
         log.error("Could not create output path: "+outputPath.toString());
         processJobFailure("Could not create output path: "+outputPath.toString(), outputPath);
         threadBouncer.release();
         return;
      }
      

      if (Files.isWritable(outputPath)) {
         //Configure paths for config file and MRF generation output

         Path dataDir = Paths.get(jobConfig.getOutputDir(), ".data");
         //Path cacheDir = Paths.get(jobConfig.getOutputDir(), ".cache");
         // Using subscriber set path for working dir to all pther non-output paths
         Path workingDir = Paths.get(jobConfig.getWorkingDir());
         Path logDir = workingDir;
         Path sipDir = Paths.get(jobConfig.getSipPath());
         if (configDir == null) {
            configPath = workingDir;
         } else {
            configPath = Paths.get(configDir);
         }
         
         String mrfProductName = jobConfig.getMrfProductName();
         
         //Creating necessary directories
         try {
            Files.createDirectories(configPath);
            Files.createDirectories(dataDir);
            Files.createDirectories(workingDir);
            Files.createDirectories(logDir);
            Files.createDirectories(sipDir);
         }
         catch(IOException e) {
            log.error("Could not create necessary sub directories: "+e.getMessage());
            processJobFailure("Could not create necessary sub directories: "+e.getMessage(), outputPath);
            threadBouncer.release();
            return;
         }

         String compression = jobConfig.getCompressionType();

         SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
         String dateString = formatter.format(jobConfig.getDateOfData());
         Path configFile = configPath.resolve(jobConfig.getParameter() + "-" + dateString + "-config.xml");
         try {
            XMLConfiguration outputConfig = new XMLConfiguration();
            outputConfig.setDelimiterParsingDisabled(true);

            //Set up properties
            outputConfig.setRootElementName("mrfgen_configuration");
            outputConfig.setProperty("date_of_data", dateString);
            outputConfig.setProperty("parameter_name", jobConfig.getParameter());
            outputConfig.setProperty("output_dir", dataDir.toString());
            //outputConfig.setProperty("cache_dir", jobConfig.getCacheDir());
            outputConfig.setProperty("working_dir", workingDir.toString());
            outputConfig.setProperty("logfile_dir", logDir.toString());
            //outputConfig.setProperty("empty_tile", jobConfig.getEmptyTile());
            outputConfig.setProperty("vrtnodata", jobConfig.getVrt());
            //TODO maybe remove??
            //outputConfig.setProperty("mrf_blocksize", jobConfig.getBlockSize());
            //outputConfig.setProperty("target_x", jobConfig.getTargetX());
            String inputString = StringUtils.join(jobConfig.getInputs(), ",");
            //outputConfig.setProperty("input_dir", "/tmp");
            outputConfig.setProperty("input_files", inputString);
            
            if(compression.startsWith("SUBDAILY-")) {
                //Hard code hack for file naming
                outputConfig.setProperty("mrf_name", "{$parameter_name}%Y%j%H%M%S_.mrf");
                outputConfig.setProperty("time_of_data", new SimpleDateFormat("HHmmss").format(jobConfig.getDateOfData()) );
                subdailyFlag = true;
                compression = compression.substring(9);
            }
            else {
                //Hard code hack for file naming
                outputConfig.setProperty("mrf_name", "{$parameter_name}%Y%j_.mrf");
                
            }
            

            outputConfig.setProperty("mrf_compression_type", compression);
            //Specify source and target EPSG
            if(jobConfig.getSourceEPSG() !=  null)
               outputConfig.setProperty("source_epsg", jobConfig.getSourceEPSG());
            if(jobConfig.getTargetEPSG() != null)
               outputConfig.setProperty("target_epsg", jobConfig.getTargetEPSG());
            
            //Specify colormap and empty tile
            if(jobConfig.getMrfEmptyTileFilename() != null) {
               Path emptyTileFile = Paths.get(jobConfig.getMrfEmptyTileFilename());
               if(Files.exists(emptyTileFile) && Files.isRegularFile(emptyTileFile))
                  outputConfig.setProperty("mrf_empty_tile_filename", jobConfig.getMrfEmptyTileFilename());
               else
                  log.debug("No empty tile specified... skipping");
               
            }
            
            if(jobConfig.getColormap() != null) {
               Path colormapFile = Paths.get(jobConfig.getColormap());
               if(Files.exists(colormapFile) && Files.isRegularFile(colormapFile))
                  outputConfig.setProperty("colormap", jobConfig.getColormap());
               else 
                  log.debug("No colormap specified... skipping");
            }

            //New generation fields
            if(jobConfig.getOutputSizeX() != null) {
               outputConfig.setProperty("target_x", jobConfig.getOutputSizeX());
            }
            if(jobConfig.getOutputSizeY() != null) {
               outputConfig.setProperty("target_y", jobConfig.getOutputSizeY());
            }
            if(jobConfig.getOverviewScale() != null && jobConfig.getOverviewLevels() != null) {
               outputConfig.setProperty("overview_levels", createOverviewString(jobConfig.getOverviewScale(), jobConfig.getOverviewLevels()));
            }
            /*
            if(jobConfig.getOverviewScale() != null) {
               outputConfig.setProperty("overview_scale", jobConfig.getOverviewScale());
            }
            if(jobConfig.getOverviewLevels() != null) {
               outputConfig.setProperty("overview_levels", jobConfig.getOverviewLevels());
            }*/
            if(jobConfig.getOverviewResample() != null) {
               outputConfig.setProperty("overview_resampling", jobConfig.getOverviewResample());
            }
            if(jobConfig.getResizeResample() != null) {
               outputConfig.setProperty("resize_resampling", jobConfig.getResizeResample());
            }
            if(jobConfig.getReprojectionResample() != null) {
               outputConfig.setProperty("reprojection_resampling", jobConfig.getReprojectionResample());
            }
            if(jobConfig.getVrtNodata() != null) {
               outputConfig.setProperty("vrtnodata", jobConfig.getVrtNodata());
            }
            if(jobConfig.getMrfBlockSize() != null) {
               outputConfig.setProperty("mrf_blocksize", jobConfig.getMrfBlockSize());
            }
            if(jobConfig.getExtents() != null) {
               outputConfig.setProperty("extents", jobConfig.getExtents());
            }
            if(jobConfig.getTargetExtents() != null) {
               outputConfig.setProperty("target_extents", jobConfig.getTargetExtents());
            }

            //Write file to disk
            outputConfig.save(configFile.toString());
         } catch (ConfigurationException ce) {
            log.error("Could not write to configuration file: " + ce.getMessage());
            processJobFailure("Could not write to configuration file: " + ce.getMessage(), outputPath);
            threadBouncer.release();
            return;
         }
         
         String pathToMrfGen = System.getProperty("generate.mrfgen.path");
         String cmd = pathToMrfGen + " -c " + configFile + " -s " + sigEventUrl+"/events/create";

         log.info("Command running: " + cmd);
         
         
         //Executing pyline command!
         Errno response = SystemProcess.execute(cmd);
         log.debug("Response obj message: " + response.getMessage());
         log.debug("Response obj id: " + response.getId());
         if (response.getId() != 0) {
            // Error handling if process did not finish. DOES NOT remove files and lets someone investigate
            log.error("MRF Generation system process invocation did not return successfuly");
            processJobFailure("MRF Generation system process invocation did not return successfully.", null);
            threadBouncer.release();
            return;
         }
         
         //TODO Again.. removing generator checks
         //Successful mrf generation... check if this job was too slow and should be deleted before sip creation.
         /*
         try {
            if(startOfToday.before(jobConfig.getDateOfData()) || startOfToday.equals(jobConfig.getDateOfData())) {
               Date lastCompleted = zk.getGenLastCompleted(jobConfig.getParameter());
               if (lastCompleted != null && lastCompleted.after(jobConfig.getTimePosted()) && !lastCompleted.equals(jobConfig.getTimePosted()) && !passFlag) {
                //Job took too long to finish, later jobs already complete. Clean up directories then process failure.
                  log.warn("Last completed job's timePosted is greater than current job's timePosted... cleaning up and ignoring:" + outputPath.toString());
                  processJobFailure("REJECTED: Last completed job's timePosted is greater than current job's timePosted... cleaning up and ignoring:" + outputPath.toString(), null);
                  threadBouncer.release();
                  return;
               }
            }
         }
         catch(IOException e) {
            log.error("Error when performing rejection lastPosted check", e);
            processJobFailure("Error when performing LastCompleted check", null);
            threadBouncer.release();
            return;
         }
         */
         
         // Checks passed.... finish it! Move files up then generate sip
         log.info("MRF generation complete! Moving data files up a directory to: "+dataDir.toString());
         DirectoryStream<Path> stream = null;
         try {
            stream = Files.newDirectoryStream(dataDir);
            for (Path file: stream) {
               String fileName = file.getFileName().toString();
               System.out.println("Moving file: "+file.toString());
               if(fileName.equals("mrfgen_previous_cycle_time.txt")) {
                  Files.delete(file);
               }
               else {
                  Files.move(file, outputPath.resolve(fileName));
               }
            }

            log.info("Deleting temporary data directory: "+dataDir.toString());
            Files.deleteIfExists(dataDir);
         }
         catch(IOException e){
            log.error("Could not move completed files up a directory from:" + dataDir.toString(), e);
            processJobFailure("Could not move completed files up a directory from:" + dataDir.toString(), outputPath);
            threadBouncer.release();
            return;
         } finally {
            if (stream != null) {
               try {
                  stream.close();
                  stream = null;
               } catch (IOException e) {

               }
            }
         }
      

         //SIP Generation code
         Path sipFile = sipDir.resolve(mrfProductName +"_"+jobConfig.getTimePosted().getTime()+".xml");

         log.info("Creating sip: "+sipFile.toString());
         ServiceProfile sipObj;
         Date completedTime = new Date();
         try {
            //Building the SIP the long way
            sipObj = ServiceProfileFactory.getInstance().createServiceProfile();
            SPSubmission submission = sipObj.createSubmission();
            SPMetadata metadata = submission.createMetadata();
            Long startTime, stopTime;
            if(subdailyFlag) {
                startTime = jobConfig.getStartTime();
                stopTime = jobConfig.getStopTime();
            }
            else {
                startTime = getStartOfDay(jobConfig.getDateOfData()).getTime();
                stopTime = getEndOfDay(jobConfig.getDateOfData()).getTime();
            }
            metadata.setProductStartTime(startTime);
            metadata.setProductStopTime(stopTime);
            SPProductHistory history = metadata.createProductHistory();
            history.setLastRevisionDate(completedTime);
            history.setVersion("1");
            history.setCreateDate(completedTime);
            history.setRevisionHistory("TBD");
            for (MGSource source : sources) {
               SPSourceProduct product = history.createSourceProduct();
               product.setMetadataRepo(SPSourceProduct.SPMetadateaRepo.valueOf("ECHO_OPENSEARCH"));
               product.setProduct(source.getProduct());
               product.setProductType(source.getProductType());
               history.addSourceProduct(product);
            }
            metadata.setProductHistory(history);
            submission.setMetadata(metadata);

            SPHeader header = submission.createHeader();
            header.setCatalogOnly(false);
            header.setCreateTime(completedTime);
            header.setProductName(mrfProductName);
            header.setProductType(jobConfig.getParameter());
            header.setOfficialName(mrfProductName);
            header.setVersion("1");
            header.setStatus("READY");
            header.setReplace(mrfProductName);

            SPOperation generateOp = header.createOperation();
            generateOp.setAgent("PRODUCT_GENERATOR");
            generateOp.setArguments("-c "+configFile);
            generateOp.setCommand("mrfgen.py");
            generateOp.setOperation("MRF_GENERATION");
            generateOp.setOperationStartTime(genStartTime);
            generateOp.setOperationStopTime(completedTime);
            header.addOperation(generateOp);
            submission.setHeader(header);
            
            SPIngest ingest = submission.createIngest();
            ingest.setOperationSuccess(true);
            try {
               stream = Files.newDirectoryStream(outputPath);
               for (Path file: stream) {
                  if(!Files.isDirectory(file)) {
                     SPIngestProductFile ingestProductFile = ingest.createIngestProductFile();
                     SPProductFile productFile = ingestProductFile.createProductFile();
                     String fileName = file.getFileName().toString();
                     String fileExtension = fileName.substring(fileName.lastIndexOf('.')+1);
                     String fileType = "DATA";
                     String fileFormat = "RAW";
                     if(fileExtension.equals("idx")) {
                        fileType = "DATA";
                        fileFormat = "RAW";
                     }
                     else if(fileExtension.equals("xml") || fileExtension.equals("mrf")) {
                        fileType = "METADATA";
                        fileFormat = "XML";
                     }
                     else if(fileExtension.equals("pjg") || fileExtension.equals("ppg")) {
                        fileType = "DATA";
                        fileFormat = "RAW";
                     }
                     else if(fileExtension.equals("vrt")) {
                        fileType = "METADATA";
                        fileFormat = "XML";
                     }
                     productFile.setFileType(SPCommon.SPFileClass.valueOf(fileType));
                     SPFile spFile = productFile.createFile();
                     spFile.setName(fileName);
                     spFile.setDataFormat(SPCommon.SPDataFormat.valueOf(fileFormat));
                     spFile.addLink("file://"+file.toString());
                     spFile.setSize(Files.size(file));
                     spFile.setChecksumType(SPCommon.SPChecksumAlgorithm.valueOf("MD5"));
                     spFile.setChecksumValue(checksum(file.toFile()));
                     productFile.setFile(spFile);
                     
                     ingestProductFile.setProductFile(productFile);
                     ingest.addIngestProductFile(ingestProductFile);
                  }
               }
            }
            catch(IOException e){
               log.error("Could not create SIP ingest product files");
            } finally {
               if (stream != null) {
                  try {
                     stream.close();
                     stream = null;
                  } catch (IOException e) {
                  }
               }
            }
            submission.setIngest(ingest);
            SPAgent origin = sipObj.createAgent();
            SPAgent target = sipObj.createAgent();
            origin.setAddress(InetAddress.getLocalHost());
            origin.setName("PRODUCT_GENERATOR");
            origin.setTime(new Date());
            
            target.setName("Manager:"+jobConfig.getParameter());
            
            sipObj.setMessageOriginAgent(origin);
            sipObj.setMessageTargetAgent(target);
            sipObj.setSubmisson(submission);

            FileUtils.writeStringToFile(sipFile.toFile(), sipObj.toString());
            
            // Everything complete here. Update engine status on ZK and move on.
            processJobSuccess("Generation completed successfully at "+completedTime);
            
         } catch (ServiceProfileException e) {
            log.error("Could not instantiate SIP object: " + e.getMessage());
            processJobFailure("Could not instantiate SIP object: " + e.getMessage(), outputPath);
            threadBouncer.release();
            return;
         } catch (IllegalArgumentException e) {
            log.error("Source Repo value not part of Enumeration: " + e.getMessage());
            processJobFailure("Source Repo value not part of Enumeration: " + e.getMessage(), outputPath);
            threadBouncer.release();
            return;
         } catch (IOException e) {
            log.error("Could not create sip file: " + sipFile);
            processJobFailure("Could not create SIP file: " + e.getMessage(), outputPath);
            threadBouncer.release();
            return;
         }
/*         //Delete configuration file
         if (deleteConfigAfterGenerate) {
            try {
               Files.delete(configFile);
            } catch (NoSuchFileException x) {
               log.error("Couldn't find config file to remove: " + configFile);
            } catch (DirectoryNotEmptyException x) {
               log.error("Directory not empty");
            } catch (IOException x) {
               // File permission problems are caught here.
               log.error("Could't delete file. Possible permission problems: " + configFile);
            }
         }*/
      } else {
         log.error("Ending thread " + t.getName() + ". No write-access to output path (" + outputPath + ")");
         processJobFailure("Ending thread " + t.getName() + ". No write-access to output path (" + outputPath + ")", outputPath);
         threadBouncer.release();
         return;
      }
      log.info("Process ended with name: " + threadName);
      threadBouncer.release();
      return;
   }

   private String checksum(File file) {
      String checksum = null;
     try {  
         checksum = DigestUtils.md5Hex(new FileInputStream(file));
     } catch (IOException ex) {
         log.warn("Could not compute MD5 checksum for file:" + file.toString());
     }
     return checksum;

   }
   
   private void processJobFailure(String msg, Path cleanupPath) {
      Date timeOfFailure = new Date();
      
      jobConfig.setTimeCompleted(timeOfFailure);
      jobConfig.setCompletedStatus("ERROR");
      jobConfig.setCompletedMessage(msg);
      
      sendSigEvent(EventType.Error, msg,msg);
      
      try {
         engineData = EngineFactory.createProfileFromMessage(zk.getGeneratorRegistration(engineData.getFederation(), engineData.getName()));
         engineData.addToRecentJobs(jobConfig);
         engineData.deleteJob(jobConfig);
         log.debug("*** DEBUG - Showing engine data right before a ZK failure update.");
         log.debug(engineData);
         zk.setGeneratorRegistration(engineData.getFederation(), engineData.getName(), engineData.toString());
         log.info("Updated engine node with failure of job");
      }
      catch(IOException f) {
         log.error("Could not update engine node: "+ f.getMessage());
         sendSigEvent(EventType.Error, "Could not update engine node: "+ f.getMessage(),"Could not update engine node: "+ f.getMessage());
      }
      
      //Also delete outputPath
      if (cleanupPath != null) {
         try {
            Files.deleteIfExists(cleanupPath);
         } catch (IOException f) {
            log.error("Could not clean up data directory after a failure: " + cleanupPath);
            sendSigEvent(EventType.Error, "Could not clean up data directory after a failure: " + cleanupPath, "Could not clean up data directory after a failure: " + cleanupPath);
         }
      }
   }
   
   private void processJobSuccess(String msg) {
      Date timeOfSuccess = new Date();
      
      jobConfig.setTimeCompleted(timeOfSuccess);
      jobConfig.setCompletedStatus("OK");
      jobConfig.setCompletedMessage(msg);
      
      try {
         engineData = EngineFactory.createProfileFromMessage(zk.getGeneratorRegistration(engineData.getFederation(), engineData.getName()));
         engineData.addToRecentJobs(jobConfig);
         engineData.deleteJob(jobConfig);
         zk.setGeneratorRegistration(engineData.getFederation(), engineData.getName(), engineData.toString());
         //Date startOfToday = getStartOfDay(new Date());
         Calendar yesterday = Calendar.getInstance();
         yesterday.add(Calendar.DATE, -1);   
         Date startOfYesterday = getStartOfDay(yesterday.getTime());
         
         if (!passFlag && (startOfYesterday.before(jobConfig.getDateOfData()) || startOfYesterday.equals(jobConfig.getDateOfData())))
            //zk.setGenLastCompleted(jobConfig.getParameter(), jobConfig.getTimePosted());
            zk.setGenLastCompleted(jobConfig.getParameter(), timeOfSuccess);
         log.info("Updated engine node with job success.");
      }
      catch(IOException f) {
         log.error("Could not update engine node: "+ f.getMessage());
      }
   }
   
   private static String createOverviewString(Integer scale,Integer levels) {
      List<Double> listLevels = new ArrayList<Double>();
      String levelsString = "";
      
      if(levels == 0) {
          levelsString = "0";
      }
      else {
          for (Integer x = 1; x<=levels;x++) {
              listLevels.add(Math.pow(scale, x));
           }
           
           Integer count = 0;
           for(Double currentLevel: listLevels) {
              if(count == 0) {
                 levelsString = String.valueOf(currentLevel.intValue());
              }
              else {
                 levelsString += " "+String.valueOf(currentLevel.intValue());
              }
              count += 1;
           }
      }
      return levelsString;
   }

   private static Date getStartOfDay(Date date) {
      return DateUtils.truncate(date, Calendar.DATE);
   }

   private static Date getEndOfDay(Date date) {
      return DateUtils.addMilliseconds(DateUtils.ceiling(date, Calendar.DATE), -1);
   }
   
   private void sendSigEvent(EventType eventType, String description, String data) {
	      //TODO remove comment
	      SigEvent sigEvent = new SigEvent(sigEventUrl);
	      sigEvent.create(eventType, this.SIG_EVENT_CATEGORY, "mrfgenerator", "mrfgenerator", this.LOCAL_HOST_NAME, description, null, data);
	      log.info("SigEvent: " + description);
   }
}
