/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.Worker
import gov.nasa.gibs.tie.handlers.common.Constants
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

public class SipsImageryHandler implements Worker {
   private static Log logger = LogFactory.getLog(SipsImageryHandler.class)

   SipsImageryConfigurator config
   boolean error = false
   boolean batch = false

   public SipsImageryHandler(String[] args) {
      logger.debug("SipsImageryHandler creating configurator")
      this.config = new SipsImageryConfigurator(args)
      logger.debug("SipsImageryHandler setup configurator to load product type info.")
      this.error = this.config.hasError()
      logger.debug("this.error = ${this.error}")
   }

   public boolean hasError() {
      return this.error
   }

   @Override
   public void setup() throws DataHandlerException {
      // register handles control-c or sig kill
      ShutdownHook sh = new ShutdownHook()
      sh.name = 'ShutdownHook'
      Runtime.runtime.addShutdownHook(sh)

   }

   @Override
   public void cleanup() throws DataHandlerException {
      try {
         this.config.productTypes.each { name, pt ->
            pt.cleanup()
         }
      } catch (InterruptedException e) {
         e.printStackTrace()
      }
   }

   @Override
   public void work() throws DataHandlerException {
      if (this.hasError()) {
         return
      }
      this.config.productTypes.each { name, pt ->
         if (pt.batch) this.batch = true

         if (pt.ready) {
            logger.debug("Product Type: ${name} started...")
            pt.work()
         }
      }
   }

   public static void main(String[] args) {
      logger.info(Constants.CLIENT_VERSION_STR)
      logger.info(Constants.COPYRIGHT)
      SipsImageryHandler handler = new SipsImageryHandler(args)
      if (handler.hasError()) {
         System.exit(-1)
      }
      logger.trace ("Call handler setup method")
      handler.setup()
      logger.trace ("Done handler setup")
      handler.work()
      logger.trace("Done performing handler walk")

      File shutdown = new File("/tmp/sips_imagery_shutdown")
      logger.debug("handler.hasError() = ${handler.hasError()}")
      if (!handler.hasError()) {
         if (!handler.batch) {
            while (true) {

               try {
                  Thread.sleep(10000);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }

               //Reload config file for new product type entries
               handler.config.reloadConfig()
               
               // checking for shutdown file
               if (shutdown.exists()) {
                  break
               }

            }
         }
      }

      logger.info("SipsImagery Handler shutting down...")

      handler.cleanup()
      if (shutdown.exists()) {
         shutdown.delete()
      }

      System.exit(0)
   }

   private class ShutdownHook extends Thread {
      @Override
      public void run() {
         try {
            cleanup()
         } catch (DataHandlerException e) {
            e.printStackTrace()
         }
      }
   }
}