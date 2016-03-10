/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.junit.Test

/**
 *
 * @author T. Huang
 * @version $Id:$
 */
class SipsImageryWalkerBatchTest extends GroovyTestCase {

   private static Log logger = LogFactory.getLog(SipsImageryWalkerBatchTest.class)

   @Test
   public void testWalkerBatch() {
      String[] args = ['-t', 'MYG09_LHD_143_SRC', '-s', '2014-08-12', '-e', '2014-08-12']
      ApplicationConfigurator configurator = new SipsImageryConfigurator(args)
      assertFalse(configurator.hasError())

      Timer timer = new Timer()
      Map<String, ProductType> pts = configurator.productTypes
      assertEquals(3, pts.size())
      pts.each { name, pt ->
         logger.info("Processing product type: ${name}")
         if (pt.isReady()) {
            if (pt.isBatch()) {
               pt.work()
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
