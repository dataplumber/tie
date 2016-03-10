package gov.nasa.horizon.inventory

class ProductTypeCharacter {

   String value

   static belongsTo = [pt:ProductType, pte:ProductTypeElement]

   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_character_id_seq']
   }
}
