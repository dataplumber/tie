package gov.nasa.gibs.inventory

class OgcLayerMatrix {

   OgcLayer layer
   OgcTileMatrixSet matrix

   static constraints = {
   }
   
   static mapping = {
      id generator: 'sequence', params: [sequence: 'ogc_layer_matrix_id_seq']
   }
}
