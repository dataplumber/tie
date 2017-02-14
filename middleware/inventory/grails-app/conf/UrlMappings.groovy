class UrlMappings {


    static mappings = {

        "/heartbeat"(controller:"productType"){action=[GET:"heartbeat"]}

        /* BEGIN: PTMT DEVELOPMENT */

      "/PTMT/submit"(controller:"Ptmt"){action=[POST:"submit", GET:"submit"]} // "/PTMT/submit/$json"(controller:"PTMT"){action=[POST:"submit", GET:"submit"]}
      "/PTMT/$formName/schema"(controller:"Ptmt"){action=[POST:"getSchema", GET:"getSchema"]}
      "/PTMT/$formName/list"(controller:"Ptmt"){action=[POST:"getList", GET:"getList"]}
      "/PTMT/$formName/byId/$id"(controller:"Ptmt"){action=[POST:"show", GET:"show"]}
      "/PTMT/$formName/byName/$name"(controller:"Ptmt"){action=[POST:"show", GET:"show"]}

        // TODO:
        // PROVIDER
        // PROJECTION
        // ELEMENT
        // COLLECTION
        // OGC
        // DATASET

        /* END: PTMT DEVELOPMENT */

        // PRODUCT TYPE
        "/sip/"(controller:"sip"){action=[POST:"addSip", GET:"addSip"]}
        "/sip/check"(controller: "sip"){action=[POST:"checkSip", GET:"checkSip"]}
        "/sip/addSip"(controller:"sip"){action=[POST:"addSip", GET:"addSip"]}
        "/productType/$id"(controller:"productType"){action=[GET:"show", POST:"show"]}
        "/productTypeByName/$name"(controller:"productType"){action=[GET:"show", POST:"show"]}
        "/productType/$id/listProducts"(controller:"productType"){action=[GET:"listProducts", POST:"listProducts"]}
        "/productType/$id/listProductsByArchiveTime"(controller:"productType"){action=[GET:"listProductsByArchiveTime", POST:"listProductsByArchiveTime"]}
        "/productType/$id/listProductsByDataDay"(controller:"productType"){action=[GET:"listProductsByDataDay", POST:"listProductsByDataDay"]}
        "/productTypeByName/$name/listProducts"(controller:"productType"){action=[GET:"listProducts", POST:"listProducts"]}
        "/productTypeByName/$name/listProductsByArchiveTime"(controller:"productType"){action=[GET:"listProductsByArchiveTime", POST:"listProductsByArchiveTime"]}
        "/productTypeByName/$name/listProductsByDataDay"(controller:"productType"){action=[GET:"listProductsByDataDay", POST:"listProductsByDataDay"]}
        "/productType/$id/latestProduct"(controller:"productType"){action=[GET:"showLatestProduct", POST:"showLatestProduct"]}
        "/productTypeByName/$name/latestProduct"(controller:"productType"){action=[GET:"showLatestProduct", POST:"showLatestProduct"]}
        "/productType/$id/policy"(controller:"productType"){action=[GET:"showPolicy", POST:"showPolicy"]}
        "/productTypeByName/$name/policy"(controller:"productType"){action=[GET:"showPolicy", POST:"showPolicy"]}
        "/productType/$id/coverage"(controller:"productType"){action=[GET:"showCoverage", POST:"showCoverage"]}
        "/productTypeByName/$name/coverage"(controller:"productType"){action=[GET:"showCoverage", POST:"showCoverage"]}
        "/productType/$id/productCount"(controller:"productType"){action=[GET:"sizeOfProduct", POST:"sizeOfProduct"]}
        "/productTypeByName/$name/productCount"(controller:"productType"){action=[GET:"sizeOfProduct", POST:"sizeOfProduct"]}
        "/productTypes"(controller:"productType"){action=[GET:"list"]}
        "/productTypes/since"(controller:"productType"){action=[GET:"newSince"]}

        // PRODUCT
        "/product/$id"(controller:"product"){action=[GET:"show", POST:"show"]}
        "/product/$id/delete"(controller:"product"){action=[POST:"delete", GET:"delete"]}
        //"/productType/$ptId/productByName/"(controller:"product"){action=[GET:"show", POST:"show", DELETE: "delete"]}
        "/productType/$ptId/productByName/$productName"(controller:"product"){action=[GET:"show", POST:"show", DELETE: "delete"]}
        "/productTypeByName/$ptName/productByName/$productName"(controller:"product"){action=[GET:"show", DELETE: "delete"]}

        "/product/$id/listOperations"(controller:"product"){action=[GET:"listOperations", POST:"listOperations"]}
        "/productTypeByName/$ptName/productByName/$productName/listOperations"(controller:"product"){action=[GET:"listOperations", POST:"listOperations"]}
        "/product/$id/operation/$operation"(controller:"product"){action=[GET:"showOperation", POST:"showOperation"]}
        "/productTypeByName/$ptName/productByName/$productName/operation/$operation"(controller:"product"){action=[GET:"showOperation", POST:"showOperation"]}
        "/productTypeByName/$ptName/productByName/$productName/status"(controller:"product"){action=[POST:"updateProductStatus"]}
        "/product/$id/status"(controller:"product"){action=[POST:"updateProductStatus"]}

        "/product/$id/archivePath"(controller:"product"){action=[POST:"fetchArchivePath", GET:"fetchArchivePath"]}
        "/productTypeByName/$ptName/productByName/$productName/archivePath"(controller:"product"){action=[POST:"fetchArchivePath", GET:"fetchArchivePath"]}

        "/product/$id/update"(controller:"product"){action=[GET:"updateProduct", POST:"updateProduct"]}
        "/product/$id/archive/status"(controller:"product"){action=[POST:"updateProductArchiveStatus", GET:"updateProductArchiveStatus"]}
        "/product/$id/archive/checksum"(controller:"product"){action=[POST:"updateProductArchiveChecksum", GET:"updateProductArchiveChecksum"]}
        "/product/$id/archive/size"(controller:"product"){action=[POST:"updateProductArchiveSize", GET:"updateProductArchiveSize"]}
        "/product/$id/rootPath"(controller:"product"){action=[POST:"updateProductRootPath", GET:"updateProductRootPath"]}

        // GIBS
        "/productType/$id/listImages"(controller:"gibs"){action=[POST:"listImages", GET:"listImages"]}
        "/productTypeByName/$name/listImages"(controller:"gibs"){action=[POST:"listImages", GET:"listImages"]}
        "/productType/$id/projection"(controller:"gibs"){action=[POST:"showProjection", GET:"showProjection"]}
        "/productTypeByName/$name/projection"(controller:"gibs"){action=[POST:"showProjection", GET:"showProjection"]}
        "/productType/$id/emptyTile"(controller:"gibs"){action=[POST:"showEmptyTile", GET:"showEmptyTile"]}
        "/productTypeByName/$name/emptyTile"(controller:"gibs"){action=[POST:"showEmptyTile", GET:"showEmptyTile"]}
        "/productType/$id/colormap"(controller:"gibs"){action=[POST:"showColormap", GET:"showColormap"]}
        "/productTypeByName/$name/colormap"(controller:"gibs"){action=[POST:"showColormap", GET:"showColormap"]}
        "/productType/$id/generation"(controller:"gibs"){action=[POST:"showGen", GET:"showGen"]}
        "/productTypeByName/$name/generation"(controller:"gibs"){action=[POST:"showGen", GET:"showGen"]}

        /*
         "/element/$id"(controller:"element"){action=[GET:"show"]}
         "/contact/$id"(controller:"contact"){action=[GET:"show"]}
         "/contacts"(controller:"contact"){action=[GET:"list"]}
         "/dataset/$id/echo"(controller:"dataset"){action=[GET:"echoGranules"]}
         "/dataset/$id/sog"(controller:"dataset"){action=[GET:"sizeOfGranule"]}
         "/dataset/$id/coverage"(controller:"dataset"){action=[GET:"coverage"]}
         "/dataset/$id/policy"(controller:"dataset"){action=[GET:"policy"]}
         "/dataset/$id/latestGranule"(controller:"dataset"){action=[GET:"latestGranule"]}
         "/dataset/$id/aip"(controller:"dataset"){action=[GET:"getAIP"]}
         "/dataset/$id/granuleSize"(controller:"dataset"){action=[GET:"sizeOfGranule"]}
         "/datasets"(controller:"dataset"){action=[GET:"list"]}
         "/dataset/$id"(controller: "dataset"){action=[GET:"show"]}
         "/dataset/$id/granuleList"(controller:"dataset"){action=[GET:"listOfGranules"]}
         "/dataset/$id/granuleReferences"(controller:"dataset"){action=[GET:"listOfGranuleReferences"]}
         "/dataset/product/$id"(controller:"dataset"){action=[GET:"findByProductId"]}
         "/sources/"(controller:"source"){action=[GET:"list"]}
         "/sensors/"(controller:"sensor"){action=[GET:"list"]}
         //"/granule/list/aip/?"(controller:"granule"){action=[GET:"findGranuleAIPList"]}//findGranuleList
         "/granules/listById"(controller:"granule"){action=[GET:"findGranuleList"]}//findGranuleList
         "/granules"(controller:"granule"){action=[GET:"list"]}//list
         "/granule/$id/granule/rootpath"(controller:"granule"){action=[POST:"updateGranuleRootPath"]}
         "/granule/$id/granule/reassociate"(controller:"granule"){action=[POST:"updateReassociateGranule"]}
         "/granule/$id/granule/element/reassociate"(controller:"granule"){action=[POST:"updateReassociateGranuleElement"]}
         "/granule/$id/reference/local"(controller:"granule"){action=[DELETE:"deleteGranuleReference"]}
         "/granule/$id/reference/path"(controller:"granule"){action=[POST:"updateGranuleReferencePath"]}
         "/granule/$id/reference/status"(controller:"granule"){action=[POST:"updateGranuleReferenceStatus"]}
         "/granule/$id/reference/newReference"(controller:"granule"){action=[POST:"addNewGranuleReference"]}
         "/granule/$id/aipReference"(controller:"granule"){action=[POST:"updateAIPRef"]}
         "/granule/$id/statAndVerify"(controller:"granule"){action=[POST:"updateGranStatusAndVerify"]}
         "/granule/$id/aipArchive"(controller:"granule"){action=[POST:"updateAIPArch"]}
         "/granule/$id/archive/checksum"(controller:"granule"){action=[POST:"updateArchiveChecksum"]}
         "/granule/$id/archive/size"(controller:"granule"){action=[POST:"updateArchiveSize"]}
         "/granule/$id/gmh/echo"(controller:"granule"){action=[POST:"updateEchoSubmitTime"]}
         "/granule/$id/gmh"(controller:"granule"){action=[GET:"gmh"]}
         "/granule/$id/spatial"(controller:"granule"){action=[GET:"spatial"]}
         "/granule/$id/archive/status"(controller:"granule"){action=[POST:"updateArchiveStatus"]}
         "/granule/$id"(controller:"granule"){action=[GET:"show",DELETE:"delete"]}
         "/granule/$id/archivePath"(controller:"granule"){action=[GET:"fetchArchivePath"]}
         "/granule/$id/status"(controller:"granule"){action=[POST:"updateGranuleStatus"]}
         "/provider/$id"(controller:"provider"){action=[GET:"show"]}
         "/providers"(controller:"provider"){action=[GET:"list"]}
         "/manifest"(controller:"manifest"){action=[POST:"processManifest",PUT:"processManifest",GET:"processManifest"]}
         "/DMTmanifest"(controller:"manifest"){action=[POST:"processManifestDMT",PUT:"processManifestDMT",GET:"processManifestDMT"]}
         */
        "/"(view:"/index")
        "500"(view:'/error')
    }

}
