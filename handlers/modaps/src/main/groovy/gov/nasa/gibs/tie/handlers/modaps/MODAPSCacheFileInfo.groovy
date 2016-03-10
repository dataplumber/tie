package gov.nasa.gibs.tie.handlers.modaps

import gov.nasa.gibs.tie.handlers.common.CacheFileInfo

class MODAPSCacheFileInfo extends CacheFileInfo {
	
	boolean equals(o) {
		if (this.is(o)) return true
		if (getClass() != o.class) return false
  
		CacheFileInfo that = (CacheFileInfo) o
  
		if (modified != that.modified) return false
		
		if(size != 0 && that.size != 0) {
			 if (size != that.size) return false
		}
		
		if (checksumAlgorithm) {
		   if (!checksumAlgorithm.equals(that.checksumAlgorithm)) return false
		   if (!checksumValue.equals(that.checksumValue)) return false
		}
		
		if (!name.equals(that.name)) return false
		if (!product.equals(that.product)) return false
  
		return true
	 }
}
