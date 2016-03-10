package gov.nasa.gibs.inventory

import gov.nasa.horizon.inventory.Provider

class Dataset {

   String remoteDatasetId
   String shortName
   String longName
   String versionLabel
   String metadataRegistry
   String metadataEndpoint
   String description
   Provider provider

   static constraints = {
      remoteDatasetId(nullable:false)
      shortName(nullable:false)
      longName(nullable:false)
      versionLabel(nullable:false)
      metadataRegistry(nullable:true)
      metadataEndpoint(nullable:true)
      description(nullable:true)
      provider(nullable:true)
   }
   
   static mapping = {
      version("revision")
      versionLabel(column:"version")
      id generator: 'sequence', params: [sequence: 'dataset_id_seq']
   }
   
}
