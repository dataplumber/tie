package gov.nasa.gibs.tie.handlers.airs

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.Worker
import gov.nasa.gibs.tie.handlers.common.Constants
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

public class AIRSHandler implements Worker {
   private static Log logger = LogFactory.getLog(AIRSHandler.class)

   AIRSConfigurator config
   boolean error = false
   boolean batch = false

   public AIRSHandler(String[] args) {
      logger.debug("AIRSHandler creating configurator")
      this.config = new AIRSConfigurator(args)
      logger.debug("AIRSHandler setup configurator to load product type info.")
      this.error = this.config.hasError()
      logger.debug ("this.error = ${this.error}")
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

      if (this.hasError()) {
         return
      }

      this.config.productTypes.each { name, pt ->
         if (pt.ready ) {
            if (pt.batch ) {
               this.batch = true
               logger.debug ("Handler operates in batch mode.")
            }
            pt.work()
         }
       }

   }

   public static void main(String[] args) {
      logger.info(Constants.CLIENT_VERSION_STR)
      logger.info(Constants.COPYRIGHT)
      AIRSHandler handler = new AIRSHandler(args)
      if (handler.hasError()) {
         System.exit(-1)
      }
      handler.setup()
      handler.work()

      File shutdown = new File("/tmp/airs_shutdown")
      logger.debug ("handler.hasError() = ${handler.hasError()}")
      logger.debug("handler.hasError() = ${handler.hasError()}")
      logger.debug ("handler.batch = ${handler.batch}")
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

      logger.info("AIRS Handler shutting down...")

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
