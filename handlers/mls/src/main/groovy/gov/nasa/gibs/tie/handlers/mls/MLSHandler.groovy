package gov.nasa.gibs.tie.handlers.mls

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.Worker
import gov.nasa.gibs.tie.handlers.common.Constants
import gov.nasa.gibs.tie.handlers.common.JobPool
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

public class MLSHandler implements Worker {
   private static Log logger = LogFactory.getLog(MLSHandler.class)

   MLSConfigurator config
   boolean error = false
   JobPool jobPool = new JobPool(256)
   boolean batch = false

   public MLSHandler(String[] args) {
      logger.debug("MLSHandler creating configurator")
      this.config = new MLSConfigurator(args)
      logger.debug("MLSHandler setup configurator to load product type info.")
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
            pt.work()
         }
      }
   }

   public static void main(String[] args) {
      logger.info(Constants.CLIENT_VERSION_STR)
      logger.info(Constants.COPYRIGHT)
      MLSHandler handler = new MLSHandler(args)
      if (handler.hasError()) {
         System.exit(-1)
      }
      handler.setup()
      handler.work()

      File shutdown = new File("/tmp/mls_shutdown")
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

      logger.info("MLS Handler shutting down...")

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
         jobPool.shutdown()
      }
   }
}