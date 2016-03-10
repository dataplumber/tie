package gov.nasa.horizon.inventory

class ProductElement {

   Boolean obligationFlag
   String scope

   static hasMany = [productCharacter:ProductCharacter, productDatetime:ProductDatetime, productInteger:ProductInteger, productReal:ProductReal]
   
   static belongsTo = [product:Product, element:ElementDd]
   
   static constraints = {
      obligationFlag(nullable:false)
      scope(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_element_id_seq']
   }
}
