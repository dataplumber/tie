/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.imagery

import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import gov.nasa.horizon.handlers.framework.*
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Implementation of the SipsImagery Product Type
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true)
class SipsImageryProductType extends ProductTypeImpl implements Worker, Runnable {

   private static Log _logger = LogFactory.getLog(SipsImageryProductType.class)

   final String name
   int interval = 0
   String sigEventURL
   String sourceURL
   List<String> filesetFilter = []
   Date startDate = new Date()
   Date endDate = null
   int minOccurs = 1
   String productNaming = null

   // set this flag to true if the product type is to operate in batch mode.
   // that is, it has fixed start and end date and only perform one time
   // harvesting.  Batch is set to true if user did not specify an endDate
   // from the command line
   boolean batch = false

   // set this flag to true to enable this product type to start harvesting
   boolean ready = false

   private List<CacheFileInfo> cache

   private ApplicationConfigurator configurator

   private Workspace workspace

   private SipsImageryDataCrawler sic

   private final ScheduledExecutorService pool


   //private final ScheduledExecutorService pool


   public SipsImageryProductType(String name) {
      this.pool = Executors.newScheduledThreadPool(1)
      this.name = name
      _logger.debug("Product type ${name} created")
   }

   public void setConfigurator(ApplicationConfigurator configurator) {
      this.configurator = configurator

      this.workspace = configurator.workspaceFactory.createWorkspace(this.name)
      if (this.configurator.repo) {
         workspace.workspaceRoot = this.configurator.repo
      }
      workspace.setup()

      _logger.debug("Load cache from ${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml")

      this.cache = CacheFileInfo.load("${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml")
      _logger.debug("Done loading cache")
   }


   @Override
   void run() {

      if (!this.endDate) {
         this.endDate = new Date()
      }

      try {
          // do the work
          _logger.debug "creating new SIPS imagery file handler"
          SipsImageryFileHandler handler = new SipsImageryFileHandler(this)
          _logger.debug "creating SIPS imagery data crawler"
          SipsImageryDataCrawler mdc =
             new SipsImageryDataCrawler(this, [sourceURL].iterator(), this.filesetFilter, handler)
          _logger.debug ("start crawling for files")
          mdc.run()
          _logger.debug ("done crawling")
          mdc.shutdown()
          if (!batch) {
             this.startDate = new Date()
             this.endDate = null
          }
          if (new File("/tmp/sips_imagery_shutdown").exists()) {
              this.pool.shutdown()
          }
      } catch (Exception e) {
         _logger.error(e.message, e)
      }
   }

   @Override
   FileHandlerFactory getFileHandlerFactory() {
      return this.configurator.fileHandlerFactory
   }

   @Override
   MetadataHarvesterFactory getMetadataHarvesterFactory() {
      return this.configurator.metadataHarvesterFactory
   }

   @Override
   ProductTypeFactory getProductTypeFactory() {
      return this.configurator.productTypeFactory
   }

   @Override
   WorkspaceFactory getWorkspaceFactory() {
      return this.configurator.workspaceFactory
   }

   @Override
   Workspace getWorkspace() {
      return this.workspace
   }

   @Override
   void setup() throws DataHandlerException {
      //this.workspace.setup()
   }

   @Override
   void work() throws DataHandlerException {
      _logger.debug("Product Type ${this.name} scheduled every ${this.interval} sec")
      if (!this.batch)
         this.pool.scheduleWithFixedDelay(this, 0, this.interval, TimeUnit.SECONDS)
      else
         this.run()
      //this.run()
      //_logger.debug("Product Type ${this.name} scheduled every ${this.interval} sec")
      //this.pool.scheduleWithFixedDelay(this, 0, this.interval, TimeUnit.SECONDS)

   }

   @Override
   void cleanup() throws DataHandlerException {
      if (this.sic) {
         this.sic.shutdown()
      }
      this.pool.shutdown()
   }

   boolean isInCache(CacheFileInfo cacheFileInfo) {
      boolean result = this.cache.contains(cacheFileInfo)
      if (result) {
         _logger.debug("${cacheFileInfo} found in cache")
      } else {
         _logger.debug("${cacheFileInfo} NOT FOUND in cache")
      }
      return result
   }

   // Returns true if added, false if updated existing cache record.
   synchronized boolean updateCache(CacheFileInfo cacheFileInfo) {
	   def added = false
	   def cacheIndex = this.cache.findIndexOf{ it.product == cacheFileInfo.product && it.name == cacheFileInfo.name } 
	   
      if (cacheIndex > -1) {
		  _logger.trace("Update existing cache record.")
		  
	  	def updatedCfi = this.cache.get(cacheIndex)
		updatedCfi.update(cacheFileInfo)
		
	  } else {
	  	added = true
	  	this.cache << cacheFileInfo
	  }
	  
	  CacheFileInfo.save(this.cache, "${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml")
	   
      return added
   }

   @Override
   List<String> getFilesetFilter() {
      return this.filesetFilter
   }

   @Override
   void addFilesetFilter(String filesetFilter) {
      this.filesetFilter.add(filesetFilter)
   }

   @Override
   void clearFilesetFilter() {
      this.filesetFilter.clear()
   }

   @Override
   Date getStart() {
      return this.startDate
   }

   @Override
   void setStart(Date start) {
      if (start)
         this.startDate = start
   }

   @Override
   Date getEnd() {
      return this.endDate
   }

   @Override
   void setEnd(Date end) {
      if (end)
         this.endDate = end
   }

   @Override
   ApplicationConfigurator getConfigurator() {
      return this.configurator
   }
}
