package gov.nasa.horizon.inventory

class ProviderResource {

   String name
   String path
   String type
   String description
   
   static belongsTo = [provider: Provider]
   
   static constraints = {
      name(nullable:false)
      path(nullable:false)
      type(nullable:false)
      description(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'provider_resource_id_seq']
   }
}
