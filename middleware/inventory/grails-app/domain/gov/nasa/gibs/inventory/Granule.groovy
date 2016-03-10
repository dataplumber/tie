package gov.nasa.gibs.inventory

class Granule {

   String remoteGranuleUr
   String metadataEndpoint

   static belongsTo = [dataset:Dataset]
   
   static constraints = {
      remoteGranuleUr(nullable:true)
      metadataEndpoint(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'granule_id_seq']
   }
}
