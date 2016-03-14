/*****************************************************************************
 * Copyright (c) 2013 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/

package gov.nasa.gibs.tie.handlers.omi

import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.Worker
import gov.nasa.gibs.tie.handlers.common.Constants
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * The OMI data handler for ingesting file products.  It also extracts
 * metadata from the ingested product for cataloging
 *
 * @author T. Huang
 * @version $Id: $
 */
public class OMIHandler implements Worker {
   private static Log logger = LogFactory.getLog(OMIHandler.class)

   OMIConfigurator config
   boolean error = false
   boolean batch = false

   /**
    * Constructor to process user commandline input and initialize the
    * framework
    *
    * @param args user input arguments
    */
   public OMIHandler(String[] args) {
      logger.debug("OMIHandler creating configurator")
      this.config = new OMIConfigurator(args)
      logger.debug("OMIHandler setup configurator to load product type info.")
      this.error = this.config.hasError()
      logger.debug("this.error = ${this.error}")
   }

   public boolean hasError() {
      return this.error
   }

   /**
    * Hook method to initialize the handler by registering shutdown handle
    *
    * @throws DataHandlerException
    */
   @Override
   public void setup() throws DataHandlerException {
      // register handles control-c or sig kill
      ShutdownHook sh = new ShutdownHook()
      sh.name = 'ShutdownHook'
      Runtime.runtime.addShutdownHook(sh)

   }

   /**
    * Hook method to get the handler ready for shutdown by cleaning up any
    * locally created cache
    *
    * @throws DataHandlerException
    */
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

   /**
    * Hook method to kick off all product type to start data fetching
    *
    * @throws DataHandlerException if any error
    */
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

   /**
    * The main method to run this handler
    *
    * @param args user input parameters
    */
   public static void main(String[] args) {
      logger.info(Constants.CLIENT_VERSION_STR)
      logger.info(Constants.COPYRIGHT)
      OMIHandler handler = new OMIHandler(args)
      if (handler.hasError()) {
         System.exit(-1)
      }
      handler.setup()

      // kick off the handler to do work.  It is assumed
      // the handler will be operate in some kind of thread system
      handler.work()

      File shutdown = new File("/tmp/omi_shutdown")
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

      logger.info("OMI Handler shutting down...")

      handler.cleanup()
      if (shutdown.exists()) {
         shutdown.delete()
      }

      System.exit(0)
   }

   /**
    * Handle for shutdown and initiate cleanup
    *
    */
   private class ShutdownHook extends Thread {
      /**
       * Method to invoke callback to handler for cleanup
       */
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