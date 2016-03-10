/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.pdr

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.junit.Test

/**
 * @author T. Huang
 * @version $Id:$
 */
class PDRWalkerBatchTest extends GroovyTestCase {

   private static Log logger = LogFactory.getLog(PDRWalkerBatchTest.class)

/*
   @Test
   public void testWalkerBatch1() {
      String[] args = ['-u', 'gibstest', '-p', 'gibstest', '-t', 'SIPS_PDR', '-s', '2013-08-30', '-e', '2014-04-08']
      ApplicationConfigurator configurator = new PDRConfigurator(args)
      assertFalse(configurator.hasError())

      Timer timer = new Timer()
      Map<String, ProductType> pts = configurator.productTypes
      assertEquals(1, pts.size())
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
*/

   @Test
   public void testWalkerBatch2() {
      String[] args = ['-u', 'gibstest', '-p', 'gibstest', '-t', 'SIPS_PDR', '-s', '2014-07-26', '-e', '2014-07-27', '-r', '/Volumes/HD-PATU3/Development/Data/tie']
      ApplicationConfigurator configurator = new PDRConfigurator(args)
      assertFalse(configurator.hasError())

      Timer timer = new Timer()
      Map<String, ProductType> pts = configurator.productTypes
      assertEquals(1, pts.size())
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
