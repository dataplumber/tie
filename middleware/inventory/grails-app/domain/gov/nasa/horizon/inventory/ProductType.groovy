package gov.nasa.horizon.inventory
import gov.nasa.gibs.inventory.Projection

class ProductType {
   
   String identifier
   String title
   Boolean purgable
   Integer purgeRate
   String description
   Provider provider
   Long lastUpdated
   
   static hasMany = [
      policy:ProductTypePolicy,
      locationPolicies:ProductTypeLocationPolicy,
      resources:ProductTypeResource,
      metadata:ProductTypeMetadata,
      elements:ProductTypeElement
   ]
   
   static constraints = {
      identifier(nullable:false)
      title(nullable:false)
      purgable(nullable:true)
      purgeRate(nullable:true)
      description(nullable:true)
      provider(nullable:true)
      lastUpdated(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_id_seq']
   }
}
