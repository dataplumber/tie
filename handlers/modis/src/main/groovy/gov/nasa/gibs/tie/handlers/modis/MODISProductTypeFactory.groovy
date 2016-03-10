package gov.nasa.gibs.tie.handlers.modis

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import gov.nasa.horizon.handlers.framework.ProductTypeFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of the MODIS Product Type factory class
 *
 * @author T. Huang
 * @version $Id: $
 */
class MODISProductTypeFactory implements ProductTypeFactory {
   private static Log log = LogFactory.getLog(MODISProductTypeFactory.class)

   public MODISProductTypeFactory() {
      log.debug("MODISProductTypeFactory created.")
   }

   @Override
   ProductType createProductType(String name) {
      MODISProductType pt = new MODISProductType(name);
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

               MODISProductType pt = new MODISProductType(productType.name.toString())

               pt.configurator = configurator

               log.debug("Done initial configuration.  Set attributes")
               pt.sourceURL = productType.sourceURL
               pt.sigEventURL = sigevent
               pt.interval = interval
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
         }
      } catch (Exception e) {
         log.error("Unable to process configuration file: ${System.getProperty(productTypeConfig)}", e)
      }
      return result
   }


}
