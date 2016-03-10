package gov.nasa.gibs.inventory
import gov.nasa.horizon.inventory.ProductType

class OgcLayer {

   ProductType pt
   String title
   String format
   
   static hasMany = [tileMatrixSet:OgcTileMatrixSet, layerBbox:OgcLayerBbox]

   static constraints = {
      pt(nullable:false)
      title(nullable:true)
      format(nullable:true)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'ogc_layer_id_seq']
   }
}
