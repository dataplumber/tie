/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.pdr

import gov.nasa.horizon.common.api.util.URIPath
import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import gov.nasa.horizon.handlers.framework.ProductTypeFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of the PDR Product Type factory class
 *
 * @author T. Huang
 * @version $Id: $
 */
class PDRProductTypeFactory implements ProductTypeFactory {
   private static Log log = LogFactory.getLog(PDRProductTypeFactory.class)

   public PDRProductTypeFactory() {
      log.debug("PDRProductTypeFactory created.")
   }

   @Override
   ProductType createProductType(String name) {
      PDRProductType pt = new PDRProductType(name);
      return pt;
   }

   @Override
   Map<String, ProductType> createProductTypes(ApplicationConfigurator conf,
                                               String productTypeConfig) {
      PDRConfigurator configurator = conf as PDRConfigurator
      Map<String, ProductType> result = [:]
      try {
         def pts = new XmlSlurper().parseText(new File(productTypeConfig).text)
         pts.productTypes.each { def productTypes ->
            String sigevent = productTypes.sigevent
            int interval = (productTypes.interval).toInteger()
			int cacheRetention = (productTypes.cacheRetention).toInteger()
            productTypes.productType.each { productType ->
                
               if(!conf.productTypes.containsKey(productType.name.toString())) {
                   PDRProductType pt = new PDRProductType(productType.name.toString())
    
				   pt.cacheRetention = cacheRetention
                   pt.configurator = configurator
    
                   log.debug("Done initial configuration.  Set attributes")
                   if (productTypes.maxPDRConnections) {
                       pt.maxPDRConnections = (productTypes.maxPDRConnections).toInteger()
                   } else {
                       pt.maxPDRConnections = 1
                   }
                   if (productTypes.maxIngestConnections) {
                       pt.maxIngestConnections = (productTypes.maxIngestConnections).toInteger()
                   } else {
                       pt.maxIngestConnections = 1
                   }
                   pt.sourceURL = productType.sourceURL
                   pt.sigEventURL = sigevent
                   pt.interval = interval
                   pt.panURL = productType.panURL
                   URIPath uriPath = URIPath.createURIPath(pt.sourceURL)
                   if (uriPath.user && uriPath.password) {
                      pt.user = uriPath.user
                      pt.pass = uriPath.password
                   } else {
                      pt.user = configurator.user
                      pt.pass = configurator.pass
                   }
    			   log.debug("username: " + pt.user + " pass: " + pt.pass)
                   log.debug("Register file filter expressions")
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
