package gov.nasa.gibs.generate.api.job.jackson;

import java.util.*;

//import java.io.*;
//import gov.nasa.horizon.generate.api.mrfgen.jackson.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

import gov.nasa.gibs.generate.api.job.*;

public class MrfGenProfileJackson implements MrfGenProfile{
   private String operation;
   private List<MGJob> jobs;
   
   public String getOperation() {
      return this.operation;
   }
   public void setOperation(String operation) {
      this.operation = operation;
   }
   
   public List<MGJob> getJobs() {
      return this.jobs;
   }
   public void setJobs(List<MGJob> jobs) {
      this.jobs = jobs;
   }
   
   public MGJob createJob() {
      return new MGJobJackson();
   }
   
   public void addToJobs(MGJob job) {
      jobs.add(job);
   }
   
   public MrfGenProfileJackson() {
      this.jobs = new ArrayList<MGJob>();
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
