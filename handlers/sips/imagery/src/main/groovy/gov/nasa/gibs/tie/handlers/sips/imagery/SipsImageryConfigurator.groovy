/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.horizon.handlers.framework.ApplicationConfigurator
import gov.nasa.horizon.handlers.framework.DataHandlerException
import gov.nasa.horizon.handlers.framework.ProductType
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Implementation os SipsImagery configurator
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true, excludes = "productTypes")

public class SipsImageryConfigurator extends ApplicationConfigurator {

   private static Log logger = LogFactory.getLog(SipsImageryConfigurator.class)

   // property names
   private static String PROP_CONFIG_FILE = 'tie.config.file'
   private static String PROP_SCRIPT = 'horizon.user.application'
   private String[] args
   private String configFile
   private String[] filteredProductTypes

   public SipsImageryConfigurator(String[] args) {
      this.args = args
      this.hasError = !this._parseArgs(args)
      logger.debug("this.hasError = ${this.hasError}")
      if (!this.hasError) {
         this.setup()
      }
   }

   protected boolean _parseArgs(String[] args) {
      boolean result = true
      def cli = new CliBuilder(
            usage: "${System.getProperty(PROP_SCRIPT)} [options]",
            header: 'options:')
      cli.h(longOpt: 'help', 'print this message')
      cli.t(args: 1, longOpt: 'productType', argName: 'productType', 'target product type')
      cli.s(args: 1, longOpt: 'start', argName: 'startDate', 'start date yyyy-MM-dd')
      cli.e(args: 1, longOpt: 'end', argName: 'endDate', 'end date yyyy-MM-dd')
      cli.r(args: 1, longOpt: 'repo', argName: 'repo', 'local repository directory')
      cli.c(args: 1, longOpt: 'config', argName: 'config', 'tie_producttypes.xml location')
      cli.p(args: 1, longOpt: 'productTypes', argName: 'productType', 'list of product types to process')

      def options = cli.parse(args)
      if (!options) {
         logger.error('Invalid input parameter(s)')
         result = false
      }

      if (options.t) {
         this.userProductType = options.t
      }

      if (options.s) {
         this.userStart = _parseDate(options.s)
         if (!userStart) {
            logger.error("Invalid start date value ${options.s}")
            result = false
         }
      }

      if (options.e) {
         this.userEnd = _parseDate(options.e)
         if (!this.userEnd) {
            logger.error("Invalid end date value ${options.s}")
            result = false
         }
      }

      if (options.r) {
         this.repo = options.r
      }
      
      if (options.c) {
          this.configFile = options.c
      }
      else {
          this.configFile = System.getProperty(PROP_CONFIG_FILE)
      }

      if (options.h || !result) {
         cli.usage()
         result = false
      }

      if (options.p) {
         this.filteredProductTypes = options.p.split(",")
      }

      logger.debug("return result ${result}")

      return result
   }

   private static Date _parseDate(String date) {
      Date result = null
      SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd')
      try {
         result = sdf.parse(date)
      } catch (ParseException e) {
         logger.debug("Invalid date value ${date}", e)
      }
      return result
   }

   protected boolean configure() throws DataHandlerException {
      boolean result = false

      logger.debug("Loading product type config from ${this.configFile}")
      this.productTypes = this.productTypeFactory.createProductTypes(this, this.configFile)
      
      logger.debug("Done loading product type configuration")

      this.productTypes.values().each { ProductType pt ->
         if (this.filteredProductTypes == null || pt.name in this.filteredProductTypes) {
            logger.debug(pt.name + " passed name filter")
            if (this.userStart) {
               pt.start = userStart
            }
            if (this.userEnd) {
               pt.end = userEnd
            }
            // set flag to enable the product type to harvest data
            if (!this.userProductType || this.userProductType.equals(pt.name)) {
               pt.ready = true
            }
            pt.setup()
            if (logger.debugEnabled) {
               logger.info("${pt.name} -> ${pt}")
            }
         }
      }

      logger.trace("Done configuration")
      return result
   }

   public boolean reloadConfig() throws DataHandlerException {
      boolean result = false

      Map<String, ProductType> newProductTypes = null

      logger.debug("Re-loading product type config from ${this.configFile}")
      newProductTypes = this.productTypeFactory.createProductTypes(this, this.configFile)

      logger.debug("Done re-loading product type configuration")

      newProductTypes.values().each { ProductType pt ->
         if (this.filteredProductTypes == null || pt.name in this.filteredProductTypes) {
            logger.debug(pt.name + " passed name filter")
            if (this.userStart) {
               pt.start = userStart
               logger.debug("set user start date: ${pt.start}")
            }
            if (this.userEnd) {
               pt.end = userEnd
               logger.debug("set user end date: ${pt.end}")
            }
            // set flag to enable the product type to harvest data
            if (!this.userProductType || this.userProductType.equals(pt.name)) {
               pt.ready = true
            }
            pt.setup()
            pt.work()
            if (logger.debugEnabled) {
               logger.info("${pt.name} -> ${pt}")
            }
         }
      }
      this.productTypes << newProductTypes
      return result
   }
}
