/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import gov.nasa.horizon.handlers.framework.ProductTypeFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of the SipsImagery Product Type factory class
 *
 * @author T. Huang
 * @version $Id: $
 */
class SipsImageryProductTypeFactory implements ProductTypeFactory {
   private static Log log = LogFactory.getLog(SipsImageryProductTypeFactory.class)

   public SipsImageryProductTypeFactory() {
      log.debug("SipsImageryProductTypeFactory created.")
   }

   @Override
   ProductType createProductType(String name) {
      SipsImageryProductType pt = new SipsImageryProductType(name);
      return pt;
   }

   @Override
   Map<String, ProductType> createProductTypes(ApplicationConfigurator configurator,
                                               String productTypeConfig) {
      Map<String, ProductType> result = [:]
      try {
         def pts = new XmlSlurper().parseText(new File(productTypeConfig).text)
         pts.productTypes.each { def productTypes ->
            String sigevent = productTypes.sigevent
            int interval = (productTypes.interval).toInteger()
            productTypes.productType.each { productType ->

               if(!configurator.productTypes.containsKey(productType.name.toString())) {
                   SipsImageryProductType pt = new SipsImageryProductType(productType.name.toString())
    
                   pt.configurator = configurator
    
                   log.debug("Done initial configuration.  Set attributes")
                   pt.sourceURL = productType.sourceURL
                   pt.sigEventURL = sigevent
                   pt.interval = interval
                   log.debug("Register file filter expressions")
                   pt.minOccurs = (productType.fileSet.@minOccurs).toInteger()
                   pt.productNaming = productType.fileSet.@productNaming ? (productType.fileSet.@productNaming).toString() : null
                   productType.fileSet.expression.each { exp ->
                      pt.addFilesetFilter(exp.toString())
                   }
    
                   log.debug("Invoke product type setup")
                   pt.setup()
    
                   log.debug("Register product type to map")
    
                   result[pt.name] = pt
                   log.debug("ProductType[${pt.name}] created.")
               }
               else {
                   log.debug("Product Type: "+productType.name.toString()+" already running. Skipping...")
               }
            }
         }
      } catch (Exception e) {
         log.error("Unable to process configuration file: ${System.getProperty(productTypeConfig)}", e)
      }
      return result
   }


}
