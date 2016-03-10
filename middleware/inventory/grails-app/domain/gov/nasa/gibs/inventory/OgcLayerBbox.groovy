package gov.nasa.gibs.inventory

class OgcLayerBbox {
   
   Projection projection
   String lowerCorner
   String upperCorner
   
   static belongsTo = [layer: OgcLayer]

   static constraints = {
      projection(nullable:false)
      lowerCorner(nullable:false)
      upperCorner(nullable:false)
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'ogc_layer_bbox_id_seq']
   }
}
