package gov.nasa.gibs.mrfgen.server;

import gov.nasa.gibs.generate.api.engine.*;
import gov.nasa.gibs.generate.api.job.*;
import gov.nasa.horizon.sigevent.api.SigEvent;
import gov.nasa.horizon.sigevent.api.EventType;
import gov.nasa.horizon.common.api.zookeeper.api.constants.RegistrationStatus;
import gov.nasa.horizon.common.api.zookeeper.api.ZkAccess;
//import gov.nasa.horizon.common.api.zookeeper.api.ZkFactory;




import gov.nasa.horizon.common.api.zookeeper.api.ZkFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.*;

public class ZkLoopThread implements Runnable {
   private static Log log = LogFactory.getLog(ZkLoopThread.class);

   //Hand me down props from Server
   private ZkAccess zk;
   private String federation;
   private String serverName;
   private String sigEventUrl;
   
   private Engine engineData;

   //Other props
   Integer maxWaitTime = 10000;
   String outputDir;
   private String LOCAL_HOST_NAME;// = InetAddress.getLocalHost().getHostName();
   private String SIG_EVENT_CATEGORY = "UNCATEGORIZED";

   //Thread max and count
   Integer threadMax;
   Semaphore threadBouncer;
   Integer threadCount = 0;

   String testJson;

   public ZkLoopThread(ZkAccess zk, String federation, String serverName, String sigEventUrl, Integer threadMax, Integer maxWaitTime, Engine engineData, String testJson) {
      this.zk = zk;
      this.federation = federation;
      this.serverName = serverName;
      this.sigEventUrl = sigEventUrl;
      this.threadMax = threadMax;
      this.maxWaitTime = maxWaitTime;
      this.engineData = engineData;

      this.testJson = testJson;
      try {
         this.LOCAL_HOST_NAME = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
         log.warn("Could not resolve hostname, using \"locahost\" instead");
         this.LOCAL_HOST_NAME = "localhost";
      }
      threadBouncer = new Semaphore(threadMax);
   }

   public void run() {
      int i = 0;
      boolean paused = false;
      processLoop: while (true) {
         if (i < maxWaitTime)
            i += 1000;
         log.debug("Polling ZK at federation " + federation);
         //check zookeeper see if engine is still registered
         if (threadBouncer.availablePermits() <= 0) {
            try {
               log.debug("Maximum number of jobs reached so going to sleep for " + i + " ms");
               Thread.sleep(i);
            } catch (InterruptedException e) {
               log.error(e.getMessage(), e);
               sendSigEvent(EventType.Error, "MRF Process Generation interrupted", e.getMessage());
            }
         } else {
            synchronized (engineData) {
               try {
                  RegistrationStatus status = zk.checkGeneratorRegistration(federation, serverName);
                  switch (status) {
                  case READY:
                     if (paused) {
                        sendSigEvent(EventType.Info, "MRF Generation engine (" + serverName + ") registered with federation (" + federation + ") has been started.", "MRF Generation engine (" + serverName + ") registered with federation (" + federation + ") has been started.");
                        i = 0;
                        paused = false;
                        engineData.setStatus("READY");
                     }
                     //poll ZK for job
                     String input = null;
                     if (testJson != null)
                        input = FileUtils.readFileToString(new File(testJson));
                     else {
                        input = zk.getGenerationJobNoBlock(federation);
                     }

                     if (input != null) {
                        i = 0;
                        //if there is a job then delete it and process it
                        log.trace("Got node: " + input);
   
                        MrfGenProfile manifest = MrfGenProfileFactory.createProfileFromMessage(input);
                        if (manifest.getOperation().equals("generate")) {
                           String name = "${manifest.jobId}:${manifest.federation}:${manifest.productType}:${manifest.product}";
                           log.trace("Working on " + name);
                           try {
                              genProcessSpawner(manifest);
                           } catch (InterruptedException e) {
                              log.error(e.getMessage(), e);
                              sendSigEvent(EventType.Error, "MRF Process Generation could not complete", e.getMessage());
                           }
                           log.trace("Finished spawning MRF generation jobs");
                        }
                     } else {
                        //if there is no job then sleep for awhile and poll again
                        log.debug("No job so going to sleep for " + i + " ms");
                        try {
                           Thread.sleep(i);
                        } catch (InterruptedException e) {
                           log.error(e.getMessage(), e);
                           sendSigEvent(EventType.Error, "MRF Process Generation interrupted", e.getMessage());
                        }
                     }
                     break;
                  case OFFLINE:
                     log.debug("Engine not registered so shutting down.");
                     break processLoop;
                  case PAUSED:
                     if (!paused) {
                        sendSigEvent(EventType.Info, "MRF Generation engine (" + serverName + ") registered with federation (" + federation + ") has been paused.", "MRF Generation engine (" + serverName + ") registered with federation (" + federation + ") has been started.");
                        i = 0;
                        paused = true;
                        engineData.setStatus("__PAUSED__");
                     }
                     log.debug("Engine has been suspended so going to sleep for " + i + " ms");
                     try {
                        Thread.sleep(i);
                     } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                        sendSigEvent(EventType.Error, "MRF Process Generation interrupted", e.getMessage());
                     }
                     break;
                  }
               } catch (IOException e) {
                  log.error("Could not retrieve generator status: " + e.getMessage());
               }
            }
         }
         if (testJson != null)
            break processLoop;
      }
   }

   private void genProcessSpawner(MrfGenProfile manifest) throws InterruptedException {
      //Thread t = Thread.currentThread();
      List<MGJob> jobs = manifest.getJobs();
      for (MGJob job : jobs) {
         threadCount++;
         List<MGSource> sources = job.getSources();
         MGJobConfig config = job.getJobConfig();
         
         threadBouncer.acquire();
         try {
            new Thread(new GenerationThread(zk, config, sources, null, sigEventUrl, threadBouncer, engineData), "MrfGenProcess" + threadCount).start();
         } catch (Exception e) {
            log.error("Could not spawn MRF generation thread");
            threadBouncer.release();
         }
      }

   }

   private void sendSigEvent(EventType eventType, String description, String data) {
      //TODO remove comment
      //SigEvent sigEvent = new SigEvent(sigEventUrl);
      //sigEvent.create(eventType, this.SIG_EVENT_CATEGORY, "ingest", "ingest", this.LOCAL_HOST_NAME, description, null, data);
      log.info("SigEvent: " + description);
   }
}
