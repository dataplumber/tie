package gov.nasa.gibs.distribute.subscriber.linkgen;

import gov.nasa.gibs.distribute.subscriber.Subscriber;
import gov.nasa.gibs.distribute.subscriber.linkgen.LinkMode;
import gov.nasa.horizon.inventory.api.InventoryApi;
import gov.nasa.horizon.inventory.api.InventoryException;
import gov.nasa.horizon.inventory.model.Product;
import gov.nasa.horizon.inventory.model.ProductArchive;
import gov.nasa.horizon.inventory.model.Source;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.*;
import java.nio.file.*;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LinkGen {
   
   static Log log = LogFactory.getLog(LinkGen.class);
   
   public static void processLinks(List<Product> products, String productTypeName, XMLConfiguration linkMapping, boolean daemonFlag, String target) {
      //Initiating inventory connection
      InventoryApi q = new InventoryApi(System.getProperty("inventory.url"));

      String destinationProductType = null;
      try {
         destinationProductType = getTargetProductType(productTypeName, linkMapping, target);
      }
      catch(ConfigurationException e) {
         log.error(e.getMessage(), e);
         return;
      }
      
      if(destinationProductType == null) {
         log.error("Could not find product type entry in link config for: "+productTypeName);
         return;
      }
      
      for(Product product: products) {
         log.debug("Processing links for product: "+product.getId());
         List<ProductArchive> productArchiveList = null;
         String basePath = null;
         try {
            productArchiveList = q.getProductArchives(product.getId());
            basePath = q.getProductArchivePath(product.getId());
         }
         catch(InventoryException e) {
            log.error("Error communicating with the Inventory WS: getImageList or getSourceList");
            return;
         }
         
         if(productArchiveList != null && productArchiveList.size() > 0) {
            Path fullPath, fullLinkPath;
            for (ProductArchive productArchive : productArchiveList) {
               String linkBasePath = basePath.replace(productTypeName, destinationProductType);
               String linkFileName = productArchive.getName().replace(productTypeName, destinationProductType);
               
               fullPath = Paths.get(basePath + File.separator + productArchive.getName());
               fullLinkPath = Paths.get(linkBasePath + File.separator + linkFileName);
               
               String type = getPtType(productTypeName, linkMapping);
               if(type.equals(LinkMode.NRT.getValue()) && Files.notExists(fullLinkPath))  {
                  try {
                     Files.createDirectories(fullLinkPath.getParent());
                     Files.createSymbolicLink(fullLinkPath, fullPath);
                     log.debug("Link successfully created at: "+fullLinkPath);
                  }
                  catch(IOException e) {
                     log.error("Could not create link at: "+fullLinkPath, e);
                     return;
                  }
               }
               else if( (type.equals(LinkMode.NRT.getValue()) && Files.exists(fullLinkPath) && isLinkOfProductType(productTypeName, fullLinkPath) )
                     || type.equals(LinkMode.SCI.getValue())) {
                  try {
                     Files.createDirectories(fullLinkPath.getParent());
                     if(Files.exists(fullLinkPath)) {
                        Files.delete(fullLinkPath);
                     }
                     Files.createSymbolicLink(fullLinkPath, fullPath);
                     log.debug("Link successfully created for type "+type+" at: "+fullLinkPath);
                  }
                  catch(IOException e) {
                     log.error("Could not create link at: "+fullLinkPath, e);
                     return;
                  }
               }
               else {
                  log.debug("No link generation required for product: "+product.getId());
               }
            }
         }
         else {
            log.warn("No files(ProductArchives) found for product:" + product.getName());
            return;
         }
         
      }
      log.info("Finished processing links for "+products.size()+" products");
   }
   
   public static String getTargetProductType(String productTypeName, XMLConfiguration linkMapping, String target) throws ConfigurationException{
      String targetProductType = null;
      
      List<HierarchicalConfiguration> productTypes = linkMapping.configurationsAt("productTypes.productType");
      log.trace("ProductTypes count is "+ productTypes.size());
      for (HierarchicalConfiguration pt : productTypes) {
         if(pt.getString("name").equals(productTypeName)) {
            if(target != null) {
               List<HierarchicalConfiguration> targets = pt.configurationsAt("targets");
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
   
   public static String getPtType(String productTypeName, XMLConfiguration linkMapping) {
      String type = null;
      
      List<HierarchicalConfiguration> productTypes = linkMapping.configurationsAt("productTypes.productType");
      log.trace("ProductTypes count is "+ productTypes.size());
      for (HierarchicalConfiguration pt : productTypes) {
         if(pt.getString("name").equals(productTypeName)) {
            type = pt.getString("linkType");
         }
      }
      return type;
   }
   
   public static Boolean isLinkOfProductType(String productTypeName, Path link) {
      Boolean result = false;
      Path originalPath;
      try {
         originalPath = Files.readSymbolicLink(link);
         if (originalPath.toString().contains(productTypeName)) {
            result = true;
         }
      }
      catch(IOException e) {
         
      }
      return result;
   }
}
