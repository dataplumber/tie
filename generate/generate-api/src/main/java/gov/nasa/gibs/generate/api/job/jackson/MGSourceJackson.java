package gov.nasa.gibs.generate.api.job.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

//import java.util.*;



import gov.nasa.gibs.generate.api.job.*;

public class MGSourceJackson implements MGSource{
   private String productType;
   private String product;
   private String repo;
   
   public String getProductType() {
      return productType;
   }
   public void setProductType(String productType) {
      this.productType = productType;
   }
   public String getProduct() {
      return product;
   }
   public void setProduct(String product) {
      this.product = product;
   }
   public String getRepo() {
      return repo;
   }
   public void setRepo(String repo) {
      this.repo = repo;
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
