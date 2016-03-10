package gov.nasa.gibs.generate.api.engine;

import java.io.File;
import java.io.IOException;

import gov.nasa.gibs.generate.api.engine.jackson.*;
import gov.nasa.gibs.generate.api.job.MGJobConfig;
import gov.nasa.gibs.generate.api.job.jackson.MGJobConfigJackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class EngineFactory {
   
   
   public static Engine createProfileFromMessage(String message) {
      ObjectMapper mapper = new ObjectMapper();
      
      SimpleModule binder = new SimpleModule("binder");
      binder.addAbstractTypeMapping(Engine.class, EngineJackson.class);
      binder.addAbstractTypeMapping(MGJobConfig.class, MGJobConfigJackson.class);
      mapper.registerModule(binder);
      
      Engine profile = null;
      try {
         profile = mapper.readValue(message, Engine.class);
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
   
   public static Engine createProfileFromMessage(File message) {
      ObjectMapper mapper = new ObjectMapper();
      
      SimpleModule binder = new SimpleModule("binder");
      binder.addAbstractTypeMapping(Engine.class, EngineJackson.class);
      binder.addAbstractTypeMapping(MGJobConfig.class, MGJobConfigJackson.class);
      mapper.registerModule(binder);
      
      Engine profile = null;
      try {
         profile = mapper.readValue(message, Engine.class);
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
   
   public static Engine createProfile() {
      return new EngineJackson();
   }
   
   public static EngineJackson createProfile(Integer maxJobs) {
      return new EngineJackson(maxJobs);
   }

}
