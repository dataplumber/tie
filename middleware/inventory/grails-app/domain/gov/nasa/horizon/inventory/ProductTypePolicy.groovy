package gov.nasa.horizon.inventory

class ProductTypePolicy {

   String dataClass
   Integer dataFrequency
   Integer dataVolume
   Integer dataDuration
   Integer dataLatency
   Integer deliveryRate
   Integer multiDay
   Boolean multiDayLink
   String accessType
   String basePathAppendType
   String dataFormat
   String compressType
   String checksumType
   String spatialType
   String accessConstraint
   String useConstraint

   static belongsTo = [pt:ProductType]

   static constraints = {
      dataClass(nullable:true, inList:[
         "ARCHIVE-DIST",
         "REMOTE-DIST"
      ])
      dataFrequency(nullable:true)
      dataVolume(nullable:true)
      dataDuration(nullable:true)
      dataLatency(nullable:true)
      deliveryRate(nullable:true)
      multiDay(nullable:true)
      multiDayLink(nullable:true)
      accessType(nullable:false, inList:[
         "OPEN",
         "PREVIEW",
         "CONTROLLED"
      ])
      basePathAppendType(nullable:false, inList:[
         "CYCLE",
         "YEAR",
         "YEAR-DOY",
         "BATCH",
         "YEAR-FLAT",
         "YEAR-MONTH-DAY",
         "YEAR-WEEK"
      ])
      dataFormat(nullable:false, inList:[
         "MRF",
         "MRF-PPNG",
         "MRF-JPG",
         "MRF-PNG",
         "MRF-SUBDAILY-PPNG",
         "MRF-SUBDAILY-PNG",
         "JPG",
         "PNG",
         "PPNG",
         "SUBDAILY-PPNG",
         "SUBDAILY-PNG",
         "TIFF",
         "SUBDAILY-TIFF",
         "GEOTIFF",
         "SUBDAILY-GEOTIFF",
         "HDF",
         "NETCDF",
         "RAW"
      ])
      compressType(nullable:false, inList:[
         "GZIP", 
         "BZIP2", 
         "ZIP",
         "NONE"
      ])
      checksumType(nullable:false, inList:["MD5", "NONE"])
      spatialType(nullable:false, inList:[
         "ORACLE",
         "POSTGRES",
         "BACKTRACK",
         "NONE"
      ])
      accessConstraint(nullable:false)
      useConstraint(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_policy_id_seq']
   }
}
