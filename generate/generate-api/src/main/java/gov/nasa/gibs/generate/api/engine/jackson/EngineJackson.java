package gov.nasa.gibs.generate.api.engine.jackson;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import gov.nasa.gibs.generate.api.engine.*;
import gov.nasa.gibs.generate.api.job.*;
import gov.nasa.gibs.generate.api.job.jackson.*;

public class EngineJackson implements Engine{

   private String name;
   private String federation;
   private Date started;
   private String status;
   private List<MGJobConfig> currentJobs;
   private List<MGJobConfig> recentJobs;
   
   private Integer MAX_JOBS;
   
   public EngineJackson() {
      this.recentJobs = new LinkedList<MGJobConfig>();
      this.currentJobs = new ArrayList<MGJobConfig>();
      this.MAX_JOBS = 20;
   }
   
   public EngineJackson(Integer maxJobs) {
      this.recentJobs = new LinkedList<MGJobConfig>();
      this.currentJobs = new ArrayList<MGJobConfig>();
      this.MAX_JOBS = maxJobs;
   }
   
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getFederation() {
      return federation;
   }

   public void setFederation(String federation) {
      this.federation = federation;
   }

   public Date getStarted() {
      return started;
   }

   public void setStarted(Date started) {
      this.started = started;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public List<MGJobConfig> getCurrentJobs() {
      return currentJobs;
   }
   
   public void addToCurrentJobs(MGJobConfig job) {
      this.currentJobs.add(job);
   }
   
   public Boolean deleteJob(Integer index) {
      return this.currentJobs.remove(index);
   }
   
   public Boolean deleteJob(MGJobConfig jobConfig) {
      Boolean deleteFlag = false;
      Iterator<MGJobConfig> jobIter = this.currentJobs.iterator();
      while(jobIter.hasNext()) {
         MGJobConfig currentJob = jobIter.next();
         if(   currentJob.getDateOfData().equals(jobConfig.getDateOfData()) && 
               currentJob.getMrfProductName().equals(jobConfig.getMrfProductName()) &&
               currentJob.getTimePosted().equals(jobConfig.getTimePosted())
               ) {
            jobIter.remove();
            deleteFlag = true;
         }
      }
      return deleteFlag;
   }

   public void clearCurrentJobs(List<MGJobConfig> recentJobs) {
      this.currentJobs = recentJobs;
   }

   public List<MGJobConfig> getRecentJobs() {
      return recentJobs;
   }
   
   public void addToRecentJobs(MGJobConfig job) {
      if (this.recentJobs.size() >= MAX_JOBS) {
         this.recentJobs.remove(0);
      }
      this.recentJobs.add(job);
   }

   public void clearRecentJobs(List<MGJobConfig> recentJobs) {
      this.recentJobs = recentJobs;
   }

   @Override
   public String toString() {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      
      String output = null;
      try {
         output = mapper.writeValueAsString(this);
      }
      catch(JsonProcessingException e) {
         //Error processing object to json
         System.out.println("Error processing object to Json");
      }
      return output;
   }
}
