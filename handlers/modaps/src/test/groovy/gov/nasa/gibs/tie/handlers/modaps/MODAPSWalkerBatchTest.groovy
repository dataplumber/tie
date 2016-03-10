package gov.nasa.gibs.tie.handlers.modaps

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: thuang
 * Date: 9/3/13
 * Time: 11:08 AM
 * To change this template use File | Settings | File Templates.
 */
class MODAPSWalkerBatchTest extends GroovyTestCase {

   private static Log logger = LogFactory.getLog(MODAPSWalkerBatchTest.class)

   @Test
   public void testWalkerBatch() {
      String[] args = ['-t', 'MOR10FSCLLDY_SRC', '-s', '2013-08-30', '-e', '2014-08-14']
      ApplicationConfigurator configurator = new MODAPSConfigurator(args)
      assertFalse(configurator.hasError())

      Timer timer = new Timer()
      Map<String, ProductType> pts = configurator.productTypes
      assertEquals(5, pts.size())
      pts.each { name, pt ->
         logger.info("Processing product type: ${name}")
         if (pt.isReady()) {
            logger.info("Product Type ${name} is ready. ${pt}")
            if (pt.isBatch()) {
               logger.info("Handling product type ${name} in batch mode.")
               pt.work()
               logger.info ("Work completed for product type ${name}")
            }
         }
      }
      logger.info("Shutting down timer tasks")
      timer.cancel()
      pts.each { name, pt ->
         logger.info("Cleaning up product type: ${name}")
         //pt.cleanup()
      }
   }

}
