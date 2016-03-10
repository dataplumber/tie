package gov.nasa.horizon.inventory

class ProductArchive {

   String type
   Long fileSize
   Boolean compressFlag
   String checksum
   String name
   String status

   static belongsTo = [product:Product]
   
   static hasMany = [archiveReference:ProductArchiveReference]
   
   static constraints = {
      type(nullable:false, inList:["DATA", "CHECKSUM", "METADATA", "IMAGE", "GEOMETADATA"])
      fileSize(nullable:false)
      compressFlag(nullable:false)
      checksum(nullable:true)
      name(nullable:false)
      status(nullable:false, inList:["ONLINE", "CORRUPTED", "DELETED", "MISSING", "IN-PROGRESS", "ANOMALY", "OFFLINE"])
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_archive_id_seq']
   }
}
