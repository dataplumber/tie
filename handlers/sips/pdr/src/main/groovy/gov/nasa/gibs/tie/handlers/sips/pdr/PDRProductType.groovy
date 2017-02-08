/*****************************************************************************
 * Copyright (c) 2014 Jet Propulsion Laboratory,
 * California Institute of Technology.  All rights reserved
 *****************************************************************************/
package gov.nasa.gibs.tie.handlers.sips.pdr

import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import gov.nasa.gibs.tie.handlers.common.JobPool
import gov.nasa.horizon.common.api.file.FileProductHandler
import gov.nasa.horizon.common.crawler.spider.Crawler
import gov.nasa.horizon.common.crawler.spider.provider.VFSCrawler
import gov.nasa.horizon.handlers.framework.*
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.commons.lang.time.DateUtils

//import java.util.concurrent.Executors
//import java.util.concurrent.ScheduledExecutorService
//import java.util.concurrent.TimeUnit

/**
 * Implementation of the PDR Product Type
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true)
class PDRProductType extends ProductTypeImpl implements Worker, Runnable {

   private static Log _logger = LogFactory.getLog(PDRProductType.class)

   final String name
   int interval = 0
   int cacheRetention
   String sigEventURL
   String sourceURL
   String panURL
   List<String> filesetFilter = []
   Date startDate = new Date()
   Date endDate = null
   String user
   String pass

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

   //private final ScheduledExecutorService pool

   public static JobPool crawlerPool = null
   public static JobPool ingestPool = null

   private int maxPDRCons = 1
   private int maxIngestCons = 1

   public PDRProductType(String name) {
      //this.pool = Executors.newScheduledThreadPool(1)
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
   public ApplicationConfigurator getConfigurator() {
      return this.configurator
   }


   void run() {
	   Date lastCachePurge
	   //Ensure cache is purged of old entries at least once per day
	   Date now = new Date()
	   if( this.cache && ( lastCachePurge == null || lastCachePurge < DateUtils.addDays(now, -1)) ) {
		   lastCachePurge = now
		   _logger.trace("Purging expired cache entries from ${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml")
		   this.cache = CacheFileInfo.load("${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml", cacheRetention)
	   }
	   
      // do the work
      _logger.debug("Product Type ${this.name}: run method")
      crawlerPool.execute("${new Date().time}@${sourceURL}") {
         Crawler crawler = new VFSCrawler(sourceURL)
         crawler.recursive = false
         if (user)
            crawler.setAuthentication(user, pass)
         crawler.registerProductHandler(new PDRFileHandler(this) as FileProductHandler)
         crawler.registerProductSelector(new gov.nasa.horizon.common.api.file.FileFilter(this.filesetFilter as String[]))
         crawler.init()
         crawler.crawl()
         crawler.stop()
      }
	  endDate = null

      if (new File("/tmp/sips_pdr_shutdown").exists()) {
         //this.pool.shutdown()
         crawlerPool.shutdown()
         ingestPool.shutdown()
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
	  this.run()
   }

   @Override
   void cleanup() {
      //this.pool.shutdown()
      crawlerPool.shutdown()
      ingestPool.shutdown()
   }

   void setMaxPDRConnections(int maxPDRCons) {
      if (!crawlerPool) {
         if (maxPDRCons > 0) {
            this.maxPDRCons = maxPDRCons
         }
         crawlerPool = new JobPool(this.maxPDRCons)
      }
   }

   int getMaxPDRConnections() {
      return this.maxPDRCons
   }

   void setMaxIngestConnections(int maxIngestCons) {
      if (!ingestPool) {
         if (maxIngestCons > 0) {
            this.maxIngestCons = maxIngestCons
         }
         ingestPool = new JobPool(this.maxIngestCons)
      }
   }

   int getMaxIngestConnections() {
      return this.maxIngestCons
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
      def cacheIndex = this.cache.findIndexOf {
         it.product == cacheFileInfo.product && it.name == cacheFileInfo.name
      }

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
      //this.filesetFilter.add('^' + filesetFilter + '$')
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
         this.startDate = start.clearTime()
   }

   @Override
   Date getEnd() {
      return this.endDate
   }

   @Override
   void setEnd(Date end) {
      if (end)
         this.endDate = end.clearTime()
   }

}
