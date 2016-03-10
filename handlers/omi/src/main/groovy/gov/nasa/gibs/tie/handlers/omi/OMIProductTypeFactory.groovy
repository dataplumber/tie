package gov.nasa.gibs.tie.handlers.omi

import gov.nasa.horizon.common.api.util.URIPath
import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import gov.nasa.horizon.handlers.framework.ProductTypeFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of the OMI Product Type factory class
 *
 * @author T. Huang
 * @version $Id: $
 */
class OMIProductTypeFactory implements ProductTypeFactory {
   private static Log log = LogFactory.getLog(OMIProductTypeFactory.class)

   public OMIProductTypeFactory() {
      log.debug("OMIProductTypeFactory created.")
   }

   @Override
   ProductType createProductType(String name) {
      OMIProductType pt = new OMIProductType(name);
      return pt;
   }

   @Override
   Map<String, ProductType> createProductTypes(ApplicationConfigurator conf,
                                               String productTypeConfig) {
      OMIConfigurator configurator = conf as OMIConfigurator
      Map<String, ProductType> result = [:]
      try {
         def pts = new XmlSlurper().parseText(new File(productTypeConfig).text)
         pts.productTypes.each { def productTypes ->
            String sigevent = productTypes.sigevent
            int interval = (productTypes.interval).toInteger()
            productTypes.productType.each { productType ->

               OMIProductType pt = new OMIProductType(productType.name.toString())

               pt.configurator = configurator

               log.debug("Done initial configuration.  Set attributes")
               pt.sourceURL = productType.sourceURL
               URIPath uriPath = URIPath.createURIPath(pt.sourceURL)
               pt.sigEventURL = sigevent
               pt.interval = interval
               if (uriPath.user && uriPath.password) {
                  pt.user = uriPath.user
                  pt.pass = uriPath.password
               } else {
                  pt.user = configurator.user
                  pt.pass = configurator.pass
               }
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
