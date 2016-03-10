/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.gibs.tie.handlers.common.JobPool
import gov.nasa.horizon.handlers.framework.ProductType
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 *
 * @author T. Huang
 * @version $Id:$
 */
class SipsImageryWalkerThreadTest extends GroovyTestCase {

   private static Log logger = LogFactory.getLog(SipsImageryWalkerThreadTest.class)

   public void testWalkerThread() {
      String[] args = ['-s', '2013-08-28']
      ApplicationConfigurator configurator = new SipsImageryConfigurator(args)
      assertFalse(configurator.hasError())
      Map<String, ProductType> pts = configurator.productTypes
      assertEquals(3, pts.size())

      JobPool jobPool = new JobPool(256)
      pts.each { name, pt ->
         if (pt.isReady()) {
            if (!pt.isBatch()) {
               if (jobPool.execute(name) {
                  pt.work()
               }) {
                  logger.info("Product Type ${name} started")
               } else {
                  logger.info("Product Type ${name} start FAILED")
               }
            } else {
               logger.info("Batch Processing product type: ${name}")
               pt.work()
            }
         }
      }

      try {
         Thread.sleep(60000);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }

      logger.info("Shutting down workers")
      pts.each { name, pt ->
         pt.cleanup()
      }
      jobPool.shutdown()

   }
}
