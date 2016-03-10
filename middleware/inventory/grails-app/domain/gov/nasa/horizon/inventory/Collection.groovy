package gov.nasa.horizon.inventory

class Collection {

   String shortName
   String longName
   Boolean aggregate
   String description
   String fullDescription
   
   static constraints = {
      shortName(nullable:false)
      longName(nullable:false)
      aggregate(nullable:false)
      description(nullable:false)
      fullDescription(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'collection_id_seq']
   }
}
