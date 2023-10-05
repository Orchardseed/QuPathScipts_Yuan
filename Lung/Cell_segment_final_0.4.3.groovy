import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.images.ImageData
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import org.slf4j.LoggerFactory
import qupath.lib.roi.RoiTools
import qupath.lib.objects.PathObjects


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()

// Set image type, if you want to do it manually, please comment out the following 2 lines
imageData.setImageType(ImageData.ImageType.FLUORESCENCE)


QP.createFullImageAnnotation(true)
def FullAnnoSeg = hierarchy.getAnnotationObjects().findAll{it.getPathClass()==null}
def Tissue = QP.getPathClass('Tissue')
FullAnnoSeg.findAll{
    it.setPathClass(Tissue)
    it.setColor(255, 0, 0)}

//def tissue_annotations = [QP.getSelectedObject()]
def tissue_annotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tissue")}
if (tissue_annotations.isEmpty()) {
    print("Cellpose, Please select a parent object!")
    return
}
//def pathObjects = QP.getSelectedObjects()
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)


// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'C:\\QuPath\\Cellpose_models_retrained_Yuan\\DAPI_retrained_1'
def cellpose = Cellpose2D.builder(pathModel)
        .channels('DAPI (DAPI)')
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(originalPixelSize)              // Resolution for detection
        .tileSize(4000)                // If your GPU can take it, make larger tiles to process fewer of them. Useful for Omnipose
//        .diameter(30)                // Average diameter of objects in px (at the requested pixel size)
        .cellExpansion(10.0)          // Approximate cells based upon nucleus expansion
        .cellConstrainScale(5)     // Constrain cell expansion using nucleus size
        .setOverlap(50)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
//        .excludeEdges()                // Clears objects toutching the edge of the image (Not of the QuPath ROI)
//        .maskThreshold(0.2)           // Threshold for the mask detection, defaults to 0.0
//        .flowThreshold(0.5)            // Threshold for the flows, defaults to 0.4
//        .simplify(1)            // Control how polygons are 'simplified' to remove unnecessary vertices
//        .classify("Cell")     // PathClass to give newly created objects
        .constrainToParent(false)    // Prevent nuclei/cells expanding beyond any parent annotations (default is true)
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
//        .createAnnotations()           // Make annotations instead of detections. This ignores cellExpansion
        .build()

cellpose.detectObjects(imageData, tissue_annotations)
println 'Cell segmentation Done!'


//// Celltype detection
QP.runObjectClassifier("DoublePos")
def Negative = QP.getPathClass("Negative")
def CD45 = QP.getPathClass("CD45")
def ST3GAL4 = QP.getPathClass("ST3GAL4")
def CD45_ST3GAL4 = QP.getPathClass("CD45: ST3GAL4")
//def unclassified = detCells.findAll{it.getPathClass() == null}

// Set the colors for each path class
def detCells = hierarchy.getDetectionObjects()
detCells.forEach{
    if (it.getPathClass() == CD45){
        it.setColor(0, 255, 255)
    } else if (it.getPathClass() == ST3GAL4) {
        it.setColor(255, 0, 0)
    } else if (it.getPathClass() == CD45_ST3GAL4) {
        it.setColor(255, 0, 255)
    } else {
        it.setPathClass(Negative)
        it.setColor(128, 128, 128)
    }
}
println 'Celltype Detection Done!'

//// Save
QP.getProjectEntry().saveImageData(imageData)


//// Create annotation from pixel classifier
QP.createFullImageAnnotation(true)
def FullAnnoMarker = hierarchy.getAnnotationObjects().findAll{it.getPathClass()==null}
def Marker = QP.getPathClass('Marker')
FullAnnoMarker.findAll{it.setPathClass(Marker)}
QP.runPlugin('qupath.lib.algorithms.TilerPlugin', '{"tileSizeMicrons":100.0,"trimToROI":true,"makeAnnotations":true,"removeParentAnnotation":true}')

def TilesAnno = hierarchy.getAnnotationObjects().findAll{it.getPathClass()==null}
def Tile = QP.getPathClass('Tile')
TilesAnno.findAll{it.setPathClass(Tile)
    it.setColor(255, 0, 0)}
def Tiles = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tile")}
QP.selectObjects(Tiles)
QP.createAnnotationsFromPixelClassifier("aSMA_1", 2.0, 0.0)
QP.createAnnotationsFromPixelClassifier("FAP_1", 2.0, 0.0)
QP.createAnnotationsFromPixelClassifier("PanCK_1", 2.0, 200.0)
println 'PixelClassifier Done!'

def intersection_aSMA_FAP = QP.getPathClass('aSMA+FAP')
Tiles.forEach{tile ->
    def tile_aSMA = tile.getChildObjects().findAll{it.getPathClass() == QP.getPathClass("aSMA")}
    def tile_FAP = tile.getChildObjects().findAll{ it.getPathClass() == QP.getPathClass("FAP")}
    tile_aSMA.forEach{ aSMA_annotation ->
        tile_FAP.forEach{ FAP_annotation ->
            def roia = aSMA_annotation.getROI()
            def roib = FAP_annotation.getROI()
            def intersectionROI = RoiTools.combineROIs(roia, roib, RoiTools.CombineOp.INTERSECT)
            if (intersectionROI != null && !intersectionROI.isEmpty()){
                def intersection = PathObjects.createAnnotationObject(intersectionROI, intersection_aSMA_FAP)
                tile.addChildObject(intersection)
            }
        }
    }
}
// Set color
def aSMA = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("aSMA")}
def FAP = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("FAP")}
def PanCK = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("PanCK")}
def aSMA_FAP = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("aSMA+FAP")}
aSMA.findAll{it.setColor(255, 255, 0)}
FAP.findAll{it.setColor(0, 255, 0)}
PanCK.findAll{it.setColor(255, 255, 255)}
aSMA_FAP.findAll{it.setColor(128, 0, 128)}
QP.resetSelection()
println 'Create annotation from pixel classifier Done!'


//Detection To Annotation
QP.detectionToAnnotationDistances(true)
println 'Detection To Annotation Done!'

//Detection To Detection
QP.detectionCentroidDistances(false)
println 'Detection To Detection Done!'

// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)
