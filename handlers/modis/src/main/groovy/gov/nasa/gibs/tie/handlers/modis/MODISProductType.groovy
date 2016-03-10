package gov.nasa.gibs.tie.handlers.modis

import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import gov.nasa.horizon.handlers.framework.*
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.util.Date;
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Implementation of the MODIS Product Type
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true)
class MODISProductType extends ProductTypeImpl implements Worker, Runnable {

   private static Log _logger = LogFactory.getLog(MODISProductType.class)

   final String name
   int interval = 0
   String sigEventURL
   String sourceURL
   List<String> filesetFilter = []
   Date startDate = new Date()
   Date endDate = null
   Date lastRetrievedDate = null
   boolean tiled = false

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

   private final ScheduledExecutorService pool


   public MODISProductType(String name) {
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

      // create URL generator
      Iterator<String> urls = new Iterator<String>() {

         int startYear = startDate.toCalendar().get(Calendar.YEAR)
         int endYear = (endDate) ? endDate.toCalendar().get(Calendar.YEAR) : new Date().toCalendar().get(Calendar.YEAR)

         public boolean hasNext() {
            if (startYear > endYear) {
               return false
            }
            return true;
         }

         public String next() {

            // create the next URL to be fetch from
            // example:
            // from: http://lance2.modaps.eosdis.nasa.gov/imagery/elements/MODIS/MOR4ODLOLLDY/${yyyy}/
            // to: http://lance2.modaps.eosdis.nasa.gov/imagery/elements/MODIS/MOR4ODLOLLDY/2013/
            String url = sourceURL.replace('${yyyy}', '%04d')

            String result = String.format(url,
                  startYear);
            _logger.debug("Ready to fetch from ${result}")

            ++startYear
            return result;
         }

         public void remove() {
            throw new UnsupportedOperationException();
         }
      };

      // do the work
      MODISFileHandler handler = new MODISFileHandler(this)
      MODISDataCrawler mdc =
         new MODISDataCrawler(this, urls, this.filesetFilter, handler)
      mdc.run()
      mdc.shutdown()
      if (!batch) {
         this.startDate = new Date()
         this.endDate = null
		 this.lastRetrievedDate = new Date()
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
   }

   @Override
   void cleanup() throws DataHandlerException {
      this.pool.shutdown()
   }

   boolean isInCache(CacheFileInfo cacheFileInfo) {
      _logger.debug("Input CacheFileInfo")
      _logger.debug("${cacheFileInfo.product} ${cacheFileInfo.name} ${cacheFileInfo.modified} ${cacheFileInfo.size} ${cacheFileInfo.checksumValue}")
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
      this.filesetFilter.add('^' + filesetFilter.replace('${yyyy}', '\\d{4}').replace('${DDD}', '\\d{3}').replaceAll('\\.', '\\\\.') + '$')
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
   void setStart(Date startDate) {
      if (startDate)
         this.startDate = startDate
   }

   @Override
   Date getEnd() {
      return this.endDate
   }

   @Override
   void setEnd(Date endDate) {
      if (endDate)
         this.endDate = endDate
   }
   
   @Override
   Date getLastRetrievedDate() {
	   return this.lastRetrievedDate;
   }
   
   @Override
   void setLastRetrievedDate(Date lastRetrievedDate) {
	   this.lastRetrievedDate = lastRetrievedDate;
   }

   @Override
   ApplicationConfigurator getConfigurator() {
      return this.configurator
   }
}
