package gov.nasa.gibs.tie.handlers.modaps

import gov.nasa.horizon.handlers.framework.*
import groovy.transform.ToString
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Implementation of the MODAPS Product Type
 *
 * @author T. Huang
 * @version $Id: $
 */
@ToString(includeNames = true, includeFields = true)
class MODAPSProductType extends ProductTypeImpl implements Worker, Runnable {

	private static Log _logger = LogFactory.getLog(MODAPSProductType.class)

	final String name
	int interval = 0
	String sigEventURL
	String sourceURL
	int lastRow
	int lastColumn
	List<String> filesetFilter = []
	Date startDate = new Date()
	Date endDate = null
	Date lastRetrievedDate = null
	boolean tiled = false
	String partialIdPrefix
	private final ScheduledExecutorService pool

	// set this flag to true if the product type is to operate in batch mode.
	// that is, it has fixed start and end date and only perform one time
	// harvesting.  Batch is set to true if user did not specify an endDate
	// from the command line
	boolean batch = false

	// set this flag to true to enable this product type to start harvesting
	boolean ready = false

	private List<MODAPSCacheFileInfo> cache

	private ApplicationConfigurator configurator

	private Workspace workspace

	public MODAPSProductType(String name) {
		this.name = name
		this.pool = Executors.newScheduledThreadPool(1)
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

		this.cache = MODAPSCacheFileInfo.load("${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml")
		_logger.debug("Done loading cache")
	}


	@Override
	void run() {
		_logger.trace ("this.endDate = ${this.endDate}")

		Date start = startDate
		if(batch || !this.lastRetrievedDate) {
			start = start.clearTime() -1
		} else {
			start = ( (this.lastRetrievedDate.day != new Date().day) ? lastRetrievedDate-2 : lastRetrievedDate-1 ).clearTime()
		}

		if (!this.endDate) {
			this.endDate = new Date()
		}
		Date end = endDate

		_logger.trace("initial lastRetrievedDate: " + this.lastRetrievedDate)
		_logger.trace("before Iterator, startDate=${startDate} endDate=${endDate}")
		// create URL generator
		Iterator<String> urls = new Iterator<String>() {
					int row = 0;
					int col = 0;

					public boolean hasNext() {
						_logger.debug ("hasNext start=${start} end=${end}")
						if ((end && start > end) || (start > new Date())) {
							return false
						}
						return true;
					}

					public String next() {
						_logger.debug ("next start=${start} end=${end}")

						def startCal = Calendar.instance
						startCal.time = start

						int year = startCal.get(Calendar.YEAR)
						int day = startCal.get(Calendar.DAY_OF_YEAR)

						// create the next URL to be fetch from
						// example:
						// from: http://lance2.modaps.eosdis.nasa.gov/imagery/subsets/RRGlobal_r${row}c${column}/${yyyy}${DDD}/
						// to: http://lance2.modaps.eosdis.nasa.gov/imagery/subsets/RRGlobal_r05c05/2013233/
						String url = sourceURL.replace('${row}', '%02d').replace('${column}', '%02d').replace('${yyyy}', '%04d').replace('${DDD}', '%03d')

						String result = String.format(url,
								row, col, year, day);
						_logger.debug("Ready to fetch from ${result}")
						_logger.debug("lastColumn ${lastColumn} lastRow ${lastRow}")

						if (row == lastRow && col == lastColumn) {
							++start
							row = 0
							col = 0
						} else {
							if (col == lastColumn) {
								++row;
								col = 0;
							} else {
								++col;
							}
						}
						return result;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};

		// do the work
		MODAPSFileHandler handler = new MODAPSFileHandler(this)
		MODAPSDataCrawler mdc =
				new MODAPSDataCrawler(urls, this.filesetFilter, handler)
		mdc.run()
		mdc.shutdown()
		if (!batch) {
			this.startDate = new Date()
			this.endDate = null
			this.lastRetrievedDate = new Date()
			_logger.trace("updated lastRetrievedDate: " + this.lastRetrievedDate)
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

	boolean isInCache(MODAPSCacheFileInfo cacheFileInfo) {
		boolean result = this.cache.contains(cacheFileInfo)
		if (result) {
			_logger.debug("${cacheFileInfo} found in cache")
		} else {
			_logger.debug("${cacheFileInfo} NOT FOUND in cache")
		}
		return result
	}

   // Returns true if added, false if updated existing cache record.
   synchronized boolean updateCache(MODAPSCacheFileInfo cacheFileInfo) {
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
	  
	  MODAPSCacheFileInfo.save(this.cache, "${workspace.getLocation(Workspace.Location.CACHE)}${File.separator}${name}.cache.xml")
	   
      return added
   }

	@Override
	List<String> getFilesetFilter() {
		return this.filesetFilter
	}

	@Override
	void addFilesetFilter(String filesetFilter) {
		this.filesetFilter.add('^' + filesetFilter.replace('${row}', '\\d{2}').replace('${column}', '\\d{2}').replace('${yyyy}', '\\d{4}').replace('${DDD}', '\\d{3}').replaceAll('\\.', '\\\\.') + '$')
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

	Date getLastRetrievedDate() {
		return this.lastRetrievedDate;
	}

	void setLastRetrievedDate(Date lastRetrievedDate) {
		this.lastRetrievedDate = lastRetrievedDate;
	}

	String getPartialIdPrefix() {
		return this.partialIdPrefix
	}

	void setPartialIdPrefix(String prefix) {
		this.partialIdPrefix = prefix
	}

	@Override
	ApplicationConfigurator getConfigurator() {
		return this.configurator
	}
}
