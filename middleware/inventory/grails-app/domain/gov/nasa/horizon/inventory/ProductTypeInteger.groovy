package gov.nasa.horizon.inventory

class ProductTypeInteger {

   Integer value
   String units

   static belongsTo = [pt:ProductType, pte:ProductTypeElement]
   
   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_integer_id_seq']
   }
}
