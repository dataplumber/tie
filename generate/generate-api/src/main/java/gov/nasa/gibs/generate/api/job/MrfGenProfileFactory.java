package gov.nasa.gibs.generate.api.job;

//import java.util.*;
import java.io.*;

import gov.nasa.gibs.generate.api.job.jackson.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.*;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

public class MrfGenProfileFactory {
   
   public static MrfGenProfile createProfileFromMessage(String message) {
      ObjectMapper mapper = new ObjectMapper();
      
      SimpleModule binder = new SimpleModule("binder");
      binder.addAbstractTypeMapping(MrfGenProfile.class, MrfGenProfileJackson.class);
      binder.addAbstractTypeMapping(MGJobConfig.class, MGJobConfigJackson.class);
      binder.addAbstractTypeMapping(MGJob.class, MGJobJackson.class);
      binder.addAbstractTypeMapping(MGSource.class, MGSourceJackson.class);
      mapper.registerModule(binder);
      
      MrfGenProfile profile = null;
      try {
         profile = mapper.readValue(message, MrfGenProfile.class);
      }
      catch(JsonParseException e) {
         System.out.println("Couldnt parse");
         // COULD NOT DESERIALIZE EXCEPTION - Add some handling
      }
      catch(JsonMappingException e) {
         System.out.println("Couldnt map");
         // COULD NOT MAP TO OBJECT EXCEPTION - Add some handling
      }
      catch(IOException e) {
         // COULD NOT OPEN FILE - Add some handling
      }
      return profile;
   }
   
   public static MrfGenProfile createProfileFromMessage(File message) {
      ObjectMapper mapper = new ObjectMapper();
      
      SimpleModule binder = new SimpleModule("binder");
      binder.addAbstractTypeMapping(MrfGenProfile.class, MrfGenProfileJackson.class);
      binder.addAbstractTypeMapping(MGJobConfig.class, MGJobConfigJackson.class);
      binder.addAbstractTypeMapping(MGJob.class, MGJobJackson.class);
      binder.addAbstractTypeMapping(MGSource.class, MGSourceJackson.class);
      mapper.registerModule(binder);
      
      MrfGenProfile profile = null;
      try {
         profile = mapper.readValue(message, MrfGenProfile.class);
      }
      catch(JsonParseException e) {
         System.out.println("Couldnt parse");
         // COULD NOT DESERIALIZE EXCEPTION - Add some handling
      }
      catch(JsonMappingException e) {
         System.out.println("Couldnt map");
         // COULD NOT MAP TO OBJECT EXCEPTION - Add some handling
      }
      catch(IOException e) {
         // COULD NOT OPEN FILE - Add some handling
      }
      return profile;
   }
   
   public static MrfGenProfile createProfile() {
      return new MrfGenProfileJackson();
   }
   
   /*public static void main(String [] args) {
      
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      SimpleModule binder = new SimpleModule("binder");
      binder.addAbstractTypeMapping(MrfGenProfile.class, MrfGenProfileJackson.class);
      binder.addAbstractTypeMapping(MGJobConfig.class, MGJobConfigJackson.class);
      binder.addAbstractTypeMapping(MGJob.class, MGJobJackson.class);
      binder.addAbstractTypeMapping(MGSource.class, MGSourceJackson.class);      
      mapper.registerModule(binder);

      //MrfGenProfile testError = createProfileFromMessage("");
      //Boolean blah = assert(testError, null);
      
      MrfGenProfile test = new MrfGenProfileJackson();
      MGJob job1 = new MGJobJackson();
      MGJobConfig config1 = new MGJobConfigJackson();
      config1.setVrt(12);
      config1.setEmptyTile("empty");
      job1.setJobConfig(config1);
      
      List<MGJob> jobList = new ArrayList<MGJob>();
      jobList.add(job1);
      
      test.setJobs(jobList);
      
      //System.out.println(output);
      
      String json = "{\"jobs\":[{\"sources\":null,\"jobConfig\":{\"dateOfData\":null,\"parameter\":null,\"inputs\":null,\"outputDir\":null,\"cacheDir\":null,\"logFileDir\":null,\"emptyTile\":\"empty\",\"vrt\":12,\"blockSize\":null,\"compressionType\":null,\"targetX\":null}}]}";
      
      MrfGenProfile test2 = null;
      test2 = MrfGenProfileFactory.createProfileFromMessage(json);
      System.out.println(test2.getJobs().get(0).getJobConfig().getEmptyTile());
      
      //Testing toString()
      System.out.println(test.getJobs().get(0));
   }*/
}
