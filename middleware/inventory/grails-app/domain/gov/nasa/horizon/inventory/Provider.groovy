package gov.nasa.horizon.inventory

class Provider {

   String shortName
   String longName
   String type
   
   static constraints = {
      shortName(nullable:false)
      longName(nullable:false)
      type(nullable:false, inList:['DATA-PROVIDER', 'DATA-CENTER', 'SCIENCE-TEAM'])
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'provider_id_seq']
   }
}
