package gov.nasa.gibs.tie.handlers.modaps

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.Worker
import gov.nasa.gibs.tie.handlers.common.Constants
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

public class MODAPSHandler implements Worker {
   private static Log logger = LogFactory.getLog(MODAPSHandler.class)

   MODAPSConfigurator config
   boolean error = false
   boolean batch = false

   public MODAPSHandler(String[] args) {
      logger.debug("MODAPSHandler creating configurator")
      this.config = new MODAPSConfigurator(args)
      logger.debug("MODAPSHandler setup configurator to load product type info.")
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
      MODAPSHandler handler = new MODAPSHandler(args)
      if (handler.hasError()) {
         System.exit(-1)
      }
      handler.setup()
      handler.work()

      File shutdown = new File("/tmp/modaps_shutdown")
      logger.debug("handler.hasError() = ${handler.hasError()}")
      if (!handler.hasError()) {
         if (!handler.batch) {
            while (true) {
               try {
                  Thread.sleep(10000);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }

               // checking for shutdown file
               if (shutdown.exists()) {
                  break
               }
            }
         }
      }

      logger.info("MODAPS Handler shutting down...")

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