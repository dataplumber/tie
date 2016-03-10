package gov.nasa.gibs.tie.handlers.omi

import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import gov.nasa.horizon.handlers.framework.*
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.util.regex.Pattern

/**
 * Implementation of the OMI Product Type
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true)
class OMIProductType extends ProductTypeImpl implements Worker {

   private static Log _logger = LogFactory.getLog(OMIProductType.class)

   final String name
   int interval = 0
   String sigEventURL
   String sourceURL
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

   private OMIDataCrawler mdc

   public OMIProductType(String name) {
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

   void run() {
      // do the work
      OMIFileHandler handler = new OMIFileHandler(this)
      if (!mdc) {
         mdc =
            new OMIDataCrawler(this, [sourceURL].iterator(), this.filesetFilter, handler)
         mdc.run()
      }
      if (!batch) {
         this.endDate = null
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
      this.run()
   }

   @Override
   void cleanup() throws DataHandlerException {
      if (this.mdc)
         this.mdc.shutdown()
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
      this.filesetFilter.add('^' + filesetFilter.replace('${yyyy}', '\\d{4}').replace('${MM}', '\\d{2}').replace('${dd}', '\\d{2}').replaceAll('\\.', '\\\\.') + '$')
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

   @Override
   ApplicationConfigurator getConfigurator() {
      return this.configurator
   }
}
