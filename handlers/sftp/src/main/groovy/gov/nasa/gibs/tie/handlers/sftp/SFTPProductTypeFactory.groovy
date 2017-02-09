package gov.nasa.gibs.tie.handlers.sftp

import gov.nasa.horizon.common.api.util.URIPath
import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import gov.nasa.horizon.handlers.framework.ProductTypeFactory
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Implementation of the gov.nasa.gibs.tie.handlers.sftp.SFTP Product Type factory class
 *
 * @author T. Huang
 * @version $Id: $
 */
class SFTPProductTypeFactory implements ProductTypeFactory {
   private static Log log = LogFactory.getLog(gov.nasa.gibs.tie.handlers.sftp.SFTPProductTypeFactory.class)

   public SFTPProductTypeFactory() {
      log.debug("gov.nasa.gibs.tie.handlers.sftp.SFTPProductTypeFactory created.")
   }

   @Override
   ProductType createProductType(String name) {
      gov.nasa.gibs.tie.handlers.sftp.SFTPProductType pt = new gov.nasa.gibs.tie.handlers.sftp.SFTPProductType(name);
      return pt;
   }

   @Override
   Map<String, ProductType> createProductTypes(ApplicationConfigurator conf,
                                               String productTypeConfig) {
      gov.nasa.gibs.tie.handlers.sftp.SFTPConfigurator configurator = conf as gov.nasa.gibs.tie.handlers.sftp.SFTPConfigurator
      Map<String, ProductType> result = [:]
      try {
         def pts = new XmlSlurper().parseText(new File(productTypeConfig).text)
         pts.productTypes.each { def productTypes ->
            String sigevent = productTypes.sigevent
			int cacheRetention = (productTypes.cacheRetention).toInteger()
            int interval = (productTypes.interval).toInteger()
            productTypes.productType.each { productType ->

               gov.nasa.gibs.tie.handlers.sftp.SFTPProductType pt = new gov.nasa.gibs.tie.handlers.sftp.SFTPProductType(productType.name.toString())

			   pt.cacheRetention = cacheRetention
               pt.configurator = configurator
			   
			   //Get bounding box values from config
			   if( !productType.bbox.equals("") ) {
				   if( !productType.bbox.left_x.equals("") ) { pt.left_x = productType.bbox.left_x.toString() as Long }
				   if( !productType.bbox.lower_y.equals("") ) { pt.lower_y = productType.bbox.lower_y.toString() as Long }
				   if( !productType.bbox.right_x.equals("") ) { pt.right_x = productType.bbox.right_x.toString() as Long }
				   if( !productType.bbox.upper_y.equals("") ) { pt.upper_y = productType.bbox.upper_y.toString() as Long }
			   }
			   if( !productType.width.equals("") ) { pt.width = productType.width.toString() as Long }
			   if( !productType.height.equals("") ) { pt.height = productType.height.toString() as Long }

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
