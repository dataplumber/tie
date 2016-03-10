package gov.nasa.horizon.inventory

class ElementDd {

   String shortName
   String longName
   String type
   String description
   Integer maxLength
   String scope

   static hasMany = [ptElement: ProductTypeElement]
   
   static constraints = {
      shortName(nullable:false)
      longName(nullable:true)
      type(nullable:false)
      description(nullable:true)
      maxLength(nullable:true)
      scope(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'element_dd_id_seq']
   }
}
