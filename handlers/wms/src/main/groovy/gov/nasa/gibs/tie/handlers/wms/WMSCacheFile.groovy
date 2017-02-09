package gov.nasa.gibs.tie.handlers.wms

import java.io.IOException
import java.util.List
import org.xml.sax.SAXException
import groovy.xml.MarkupBuilder
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.junit.After;
import java.util.regex.Matcher
import java.util.regex.Pattern
import gov.nasa.gibs.tie.handlers.common.CacheFileInfo


class WMSCacheFile {
	private static Log _logger = LogFactory.getLog(WMSCacheFile.class)

	private boolean cmrMode = true
	private Set<CacheFileInfo> cacheEntries
	private long lastRetrieved //Timestamp of the last time the handler retrieved data
	Set<String> retrievedDataIds //List of IDs associated with the last retrieved data

	public WMSCacheFile(boolean cmrMode, Set<String> retrievedDataIds = null) {
		this.cmrMode = cmrMode
		this.cacheEntries = new LinkedHashSet<CacheFileInfo>()
		this.retrievedDataIds = retrievedDataIds
		//lastRetrieved = System.currentTimeMillis()
	}
	
	public void load(String cachefile, int cacheRetention) throws IOException {
		boolean deletedCacheEntry = false
		Set<String> worldFilesToDelete = new HashSet<String>()
		this.retrievedDataIds = new LinkedHashSet<String>()
		
		def crawlercache
		//TODO do something if file doesn't exist?
		if (new File(cachefile).exists()) {
			try {
				crawlercache = new XmlSlurper().parseText(new File(cachefile).text)
				
				this.lastRetrieved = Long.parseLong(crawlercache.lastRetrieved.toString())
				
				crawlercache.retrieveddataids.id.each { currId ->
					this.retrievedDataIds.add(currId)		
				}
				
				crawlercache.fileinfo.each { fileinfo ->
					CacheFileInfo info = new CacheFileInfo()
					info.product = fileinfo.product as String
					info.name = fileinfo.name as String
					info.modified = Long.parseLong(fileinfo.modified.toString())
					info.size = Long.parseLong(fileinfo.size.toString())
					//info.granuleId = fileinfo.granuleId as String
					
					if (fileinfo.checksum) {
						info.checksumAlgorithm = fileinfo.checksum.algorithm as String
						info.checksumValue = fileinfo.checksum.value as String
					}		
					Pattern p = Pattern.compile("(\\S)*(?=\\.)")
					Matcher m = p.matcher(info.name)
					String baseFileName = ""
					if(m.find()) {baseFileName = m.group(0)}
					
					Calendar cal = Calendar.getInstance()
					cal.add(Calendar.DATE, -1*cacheRetention)
					if( info.modified < cal.getTimeInMillis() || worldFilesToDelete.contains(baseFileName)) {
						deletedCacheEntry = true
						worldFilesToDelete.add(baseFileName)
						//this.retrievedDataIds.remove(info.granuleId)
					} else {
						this.cacheEntries.add(info)
					}
				}
				if(deletedCacheEntry) {
					save(this.cacheEntries, cachefile, false)
				}
			} catch (Exception e) {
				_logger.error("Unable to process cache file ${cachefile}. Handler will now terminate.", e)
				System.exit(-1)
			}
		}
	}

	public void save(Set<CacheFileInfo> fileInfos, def cachefile, boolean updateLastRetrieved = true)
	throws IOException {
		if(fileInfos == null || fileInfos.size == 0) {
			fileInfos = this.cacheEntries
		}
		if(updateLastRetrieved) {
			this.lastRetrieved = System.currentTimeMillis()
		}
		
		def writer = new FileWriter(new File(cachefile))
		def xml = new MarkupBuilder(writer)
		xml.crawlercache() {
			
			lastRetrieved(this.lastRetrieved)
			if(cmrMode) {
				retrieveddataids() {
					retrievedDataIds.each { currId ->
						//_logger.debug("writing granuleID to cache file: ${currId}")
						id(currId)	
					}
				}
			}
			
			fileInfos.each { fi ->
				fileinfo() {
					product(fi.product)
					name(fi.name)
					modified(fi.modified)
					size(fi.size)
					if (fi.checksumAlgorithm) {
						checksum() {
							algorithm(fi.checksumAlgorithm)
							value(fi.checksumValue)
						}
					}
				}
			}
		}
	}
	
	public void addDataId(String id) {
		if(this.retrievedDataIds == null || this.retrievedDataIds.size() < 1) {
			this.retrievedDataIds = new HashSet<String>()
		}
		
		this.retrievedDataIds.add(id)
	}
}