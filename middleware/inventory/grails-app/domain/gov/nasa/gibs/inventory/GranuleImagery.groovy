package gov.nasa.gibs.inventory
import gov.nasa.horizon.inventory.Product

class GranuleImagery {

   static belongsTo = [granule: Granule, product:Product]

   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'granule_imagery_id_seq']
   }
}
