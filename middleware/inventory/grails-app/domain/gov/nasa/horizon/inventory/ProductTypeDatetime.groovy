package gov.nasa.horizon.inventory

class ProductTypeDatetime {

   Long valueLong

   static belongsTo = [pt:ProductType, pte:ProductTypeElement]
   
   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'product_type_datetime_id_seq']
   }
}
