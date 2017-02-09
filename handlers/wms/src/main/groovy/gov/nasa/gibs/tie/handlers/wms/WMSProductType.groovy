package gov.nasa.gibs.tie.handlers.wms

import gov.nasa.gibs.tie.handlers.common.CacheFileInfo
import gov.nasa.horizon.handlers.framework.*
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat

import org.apache.commons.lang.time.DateUtils

/**
 * Implementation of the WMS Product Type
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true)
class WMSProductType extends ProductTypeImpl implements Worker, Runnable {

	private static Log _logger = LogFactory.getLog(WMSProductType.class)

	final String name
	int interval = 0
	String detectionType
	int cacheRetention
	String sigEventURL
	String cmrURL
	String sourceURL
	SimpleDateFormat wmsDateTimeFormat
    
    Long left_x
    Long right_x
    Long upper_y
    Long lower_y
    
    Long height
    Long width

	String fileExtension
    
	List<String> filesetFilter = []
	Date startDate = null
	Date endDate = null
	boolean userEnd = false
	boolean stop = false
	String queryType

	private final ScheduledExecutorService pool

	// set this flag to true if the product type is to operate in batch mode.
	// that is, it has fixed start and end date and only perform one time
	// harvesting.  Batch is set to true if user did not specify an endDate
	// from the command line
	boolean batch = false

	// set this flag to true to enable this product type to start harvesting
	boolean ready = false

	private WMSCacheFile cacheFile

	private ApplicationConfigurator configurator

	private Workspace workspace

	public WMSProductType(String name) {
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

		this.cacheFile = new WMSCacheFile(this.detectionType.equals("CMR"))
		this.cacheFile.load("${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml", cacheRetention)
		_logger.debug("Done loading cache")
	}


	@Override
	void run() {
		Date lastCachePurge
		Date start

		if(this.startDate == null) {
			start = cacheFile.lastRetrieved > 0 ? new Date(this.cacheFile.lastRetrieved) : new Date().clearTime()
		} else {
			start = this.startDate.clearTime()
		}
		
		if (!this.endDate) {
			this.endDate = new Date()
		}
		//Date end = this.endDate.clearTime()
		Date end = DateUtils.addMilliseconds(DateUtils.ceiling(this.endDate, Calendar.DATE), -1)

		Date startDay = start
		Date endDay = end

		// create the URL to be fetched from
		String result
		Iterator<String> urls
		
		if(this.detectionType.equals("NRT")) {
			// create URL generator
			urls = new Iterator<String>() {
	  
			   public boolean hasNext() {
				  if (startDay > endDay) {
					 return false
				  }
				  return true;
			   }
	  
			   public String next() {
				  // create the next URL to be fetch from
				   result = sourceURL.replace('${time}', wmsDateTimeFormat.format(startDay).toString())
				  _logger.debug("Ready to fetch from ${result}")
	  
				  startDay = startDay + 1
				  _logger.debug("incrementing startDay. startDay= ${startDay}")
				  
				  return result
			   }
	  
			   public void remove() {
				  throw new UnsupportedOperationException();
			   }
			};
		} else if(this.detectionType.equals("CMR")) {
			String url = cmrURL.replace('${yyyy}', '%04d').replace('${MM}', '%02d').replace('${dd}', '%02d')
			
			result = String.format(url,
					startDay.toCalendar().get(Calendar.YEAR), startDay.toCalendar().get(Calendar.MONTH) + 1, startDay.toCalendar().get(Calendar.DAY_OF_MONTH));
	
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			if(this.queryType == 'revision') {
				result += "&sort_key[]=revision_date&revision_date[]=" + format.format(startDay)
			} else {
				result += "&sort_key[]=start_date&temporal[]=" + format.format(startDay)
			}
	
			if(this.userEnd) {
				result += ",${format.format(endDay)}"
			}
		}

		//Ensure cache is purged of old entries at least once per day
		Date now = new Date()
		_logger.debug("Cache was last purged at: ${lastCachePurge}")
		if( this.cacheFile && (lastCachePurge == null || lastCachePurge < DateUtils.addDays(now, -1)) ) {
			lastCachePurge = now

			_logger.debug("Purging expired cache entries from ${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml")
			this.cacheFile.load("${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml", cacheRetention)
			}

		_logger.debug("Ready to fetch from ${result}")
		
		// do the work
		WMSFileHandler handler = new WMSFileHandler(this)
		WMSDataCrawler cdc
		if(this.detectionType.equals("CMR")) {
			cdc = new WMSDataCrawler(this, result, null, handler)
		} else { //NRT mode
			cdc = new WMSDataCrawler(this, urls, this.filesetFilter, handler)
		}
		cdc.run()
		cdc.shutdown()
		if (!batch) { 
			this.startDate = null
			this.endDate = null
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
		_logger.info("Product Type ${this.name} scheduled every ${this.interval} sec")
		if (!this.batch)
			this.pool.scheduleWithFixedDelay(this, 0, this.interval, TimeUnit.SECONDS)
		else
			this.run()
	}

	@Override
	void cleanup() {
		try {
			this.pool.shutdown()
		}
		catch (DataHandlerException e) {
			_logger.error(e.message, e)
		}
	}

	boolean isInCache(CacheFileInfo cacheFileInfo) {
		def cacheEntries = this.cacheFile.cacheEntries
		boolean result = cacheEntries.contains(cacheFileInfo)
		if (result) {
			_logger.debug("${cacheFileInfo} found in cache")
		} else {
			_logger.debug("${cacheFileInfo} NOT FOUND in cache")
		}
		return result
	}

	boolean isInCacheIgnoreTimestamp(CacheFileInfo cacheFileInfo) {
		def cacheEntries = this.cacheFile.cacheEntries
		boolean result = false

		if(cacheEntries.size() == 0)
			return result

		result = cacheEntries.any{
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
	synchronized boolean updateCache(CacheFileInfo cacheFileInfo, String granuleId, boolean updateLastRetrieved = true) {
		def cacheEntries = this.cacheFile.cacheEntries
		def added = false

		def cacheIndex = cacheEntries.size() == 0 ? -1 : cacheEntries.findIndexOf{ it.product == cacheFileInfo.product && it.name == cacheFileInfo.name }
		if (cacheIndex > -1) {
			_logger.trace("Update existing cache record.")

			def updatedCfi = cacheEntries.get(cacheIndex)
			updatedCfi.update(cacheFileInfo)

		} else {
			added = true
			cacheEntries.add(cacheFileInfo)
			this.cacheFile.addDataId(granuleId)
		}

		this.cacheFile.save(cacheEntries, "${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml", updateLastRetrieved)

		return added
	}
	
	void initializeWmsDateTimeFormat(String format) {
		this.wmsDateTimeFormat = new SimpleDateFormat(format)
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
}
