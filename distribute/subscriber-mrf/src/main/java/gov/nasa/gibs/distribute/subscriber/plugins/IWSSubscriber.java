package gov.nasa.gibs.distribute.subscriber.plugins;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.nasa.gibs.distribute.subscriber.api.DataSubscriber;
import gov.nasa.gibs.distribute.subscriber.plugins.*;
import gov.nasa.horizon.inventory.model.*;
import gov.nasa.horizon.inventory.api.*;

public class IWSSubscriber implements DataSubscriber {

   static Log _logger = LogFactory.getLog(IWSSubscriber.class);

   public List<Product> list(ProductType productType, Date lastRunTime) {
      _logger.debug("Running queries with last run time: " + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS").format(lastRunTime));
      String IWS_URL = System.getProperty("inventory.url");
      InventoryApi q = new InventoryApi(IWS_URL);

      //_logger.info("Fetching products for productType "+ productType.getIdentifier());
      
      List<Product> listOfProducts = null;
      try {
         listOfProducts = q.getProductsByArchiveTime(productType.getIdentifier(), lastRunTime, null, true); //String ptName, Date start, Date stop, Integer page, Boolean onlineOnly=false
      } catch (Exception e) {
         _logger.error("Error connecting to inventory Service. Could not retrieve list of granules.", e);
         _logger.error(e.getMessage());
      }
      
      return listOfProducts;

   }
   
   public List<Product> listRange(ProductType productType, Date start, Date end) {
      SimpleDateFormat timeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
      _logger.info("Running query for "+productType.getIdentifier()+" with range from: " + timeFormatter.format(start) + " TO "+timeFormatter.format(end));
      String IWS_URL = System.getProperty("inventory.url");
      InventoryApi q = new InventoryApi(IWS_URL);

      _logger.info("Fetching products for productType "+ productType.getIdentifier());

      List<Product> listOfProducts = null;
      try {
         listOfProducts = q.getProductIdListAll(productType.getIdentifier(), start, end, null, true); //String ptName, Date start, Date stop, Integer page, Boolean onlineOnly=false
      } catch (Exception e) {
         _logger.error("Error connecting to inventory Service. Could not retrieve list of granules.", e);
      }
      
      return listOfProducts;
   }
}
