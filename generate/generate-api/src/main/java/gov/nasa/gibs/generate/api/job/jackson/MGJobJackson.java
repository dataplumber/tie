package gov.nasa.gibs.generate.api.job.jackson;

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import gov.nasa.gibs.generate.api.job.*;

public class MGJobJackson implements MGJob{
   private List<MGSource> sources;
   private MGJobConfig config;
   
   public List<MGSource> getSources() {
      return this.sources;
   }
   public void setSources(List<MGSource> sources) {
      this.sources = sources;
   }
   public MGSource createSource() {
      return new MGSourceJackson();
   }
   public void addToSources(MGSource source) {
      sources.add(source);
   }
   

   public MGJobConfig getJobConfig() {
      return this.config;
   }
   public void setJobConfig(MGJobConfig config) {
      this.config = config;
   }
   public MGJobConfig createJobConfig() {
      return new MGJobConfigJackson();
   }
   
   public MGJobJackson() {
      this.sources = new ArrayList<MGSource>();
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
