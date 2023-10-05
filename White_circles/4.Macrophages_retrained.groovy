import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.images.ImageData
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import org.slf4j.LoggerFactory


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def tissue_annotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tissues")}
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

// Set image type, if you want to do it manually, please comment out the following 2 lines
imageData.setImageType(ImageData.ImageType.FLUORESCENCE)

//// Create annotation
//QP.createSelectAllObject(true)

// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'D:\\QuPath\\QuPath projects\\BIF\\yg.bif\\Script\\White_circles\\models\\DAPI_1'
def cellpose = Cellpose2D.builder(pathModel)
        .channels('DAPI')
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
        .useGPU()
        .build()

cellpose.detectObjects(imageData, tissue_annotations)
println 'Cell segmentation Done!'


// Celltype detection
QP.runObjectClassifier("CD68_CLEC4F_classifier")
println 'Celltype detection Done!'


// Lipids detection
QP.selectAnnotations()
QP.createDetectionsFromPixelClassifier("Lipids_2", 0.0, 0.0, "SPLIT")
println 'Lipids detection Done!'

QP.selectDetections()
QP.addShapeMeasurements("AREA", "LENGTH", "CIRCULARITY", "SOLIDITY", "MAX_DIAMETER", "MIN_DIAMETER", "NUCLEUS_CELL_RATIO")
QP.resetSelection()
println 'Add shape measurements Done!'

//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)

