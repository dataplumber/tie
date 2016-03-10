package gov.nasa.gibs.tie.handlers.airs

import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import gov.nasa.horizon.handlers.framework.*
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.util.Date;
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern;

/**
 * Implementation of the AIRS Product Type
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true)
class AIRSProductType extends ProductTypeImpl implements Worker, Runnable {

   private static Log _logger = LogFactory.getLog(AIRSProductType.class)

   final String name
   int interval = 0
   String sigEventURL
   String sourceURL
   List<String> filesetFilter = []
   Date startDate = new Date()
   Date endDate = null
   Date lastRetrievedDate = null
   boolean stop = false
   private final ScheduledExecutorService pool

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

   public AIRSProductType(String name) {
      this.name = name
      _logger.debug("Product type ${name} created")
      this.pool = Executors.newScheduledThreadPool(1)
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
	   _logger.debug("beginning of run(), startDate: ${this.startDate} | endDate: ${this.endDate} | lastRetrievedDate: ${this.lastRetrievedDate}")
	   
		Date start = this.startDate
		if(batch || !this.lastRetrievedDate) {
			start = start.clearTime() -1
		} else {
			start = ( (this.lastRetrievedDate.day != new Date().day) ? lastRetrievedDate-2 : lastRetrievedDate-1 ).clearTime()
		}
		
		if (!this.endDate) {
	         this.endDate = new Date()
	      }
      Date end = this.endDate

	  _logger.debug("Start: ${start} | End: ${end}")
	  
      // create URL generator
      Iterator<String> urls = new Iterator<String>() {

         Date startDay = start
         Date endDay = end

         public boolean hasNext() {
            if (startDay > endDay) {
               return false
            }
            return true;
         }

         public String next() {

            // create the next URL to be fetch from
            String url = sourceURL.replace('${yyyy}', '%04d').replace('${MM}', '%02d').replace('${dd}', '%02d')

            String result = String.format(url,
                  startDay.toCalendar().get(Calendar.YEAR), startDay.toCalendar().get(Calendar.MONTH) + 1, startDay.toCalendar().get(Calendar.DAY_OF_MONTH));
            _logger.debug("Ready to fetch from ${result}")

            startDay = startDay + 1
			_logger.debug("incrementing startDay. startDay= ${startDay}")
            return result
         }

         public void remove() {
            throw new UnsupportedOperationException();
         }
      };

      // do the work
      AIRSFileHandler handler = new AIRSFileHandler(this)
      AIRSDataCrawler mdc =
         new AIRSDataCrawler(this, urls, this.filesetFilter, handler)
      mdc.run()
      mdc.shutdown()
      if (!batch) {
         this.startDate = new Date()
         this.endDate = null
         this.lastRetrievedDate = new Date()
		 _logger.debug("Set lastRetrievedDate: ${this.lastRetrievedDate}")
      }
   }

   @Override
   public ApplicationConfigurator getConfigurator() {
      return this.configurator
   }

   @Override
   public Workspace getWorkspace() {
      return this.workspace
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
      boolean result = this.cache.contains(cacheFileInfo)
      if (result) {
         _logger.debug("${cacheFileInfo} found in cache")
      } else {
         _logger.debug("${cacheFileInfo} NOT FOUND in cache")
      }
      return result
   }
   
   boolean isInCacheIgnoreTimestamp(CacheFileInfo cacheFileInfo) {
	  boolean result = this.cache.any{ 
		  it.product == cacheFileInfo.product && 
		  it.name == cacheFileInfo.name && 
		  it.size == cacheFileInfo.size &&
		  it.checksumAlgorithm == cacheFileInfo.checksumAlgorithm &&
		  it.checksumValue == cacheFileInfo.checksumValue
	  }
	  
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
   Date getLastRetrievedDate() {
	   return this.lastRetrievedDate
   }

   @Override
   void setLastRetrievedDate(Date lastRetrievedDate) {
	   this.lastRetrievedDate = lastRetrievedDate
   }

}
