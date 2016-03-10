package gov.nasa.horizon.inventory

class ProductTypeElement {

   Boolean obligationFlag
   String scope
   
   static belongsTo = [pt:ProductType, element:ElementDd]
   
   static hasMany = [ptCharacter:ProductTypeCharacter, ptDatetime:ProductTypeDatetime, ptIntegerm:ProductTypeInteger, ptReal:ProductTypeReal]

   static constraints = {
      element(nullable:false)
      obligationFlag(nullable:false)
      scope(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_element_id_seq']
   }
}
