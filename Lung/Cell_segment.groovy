import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.images.ImageData
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import org.slf4j.LoggerFactory


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)
// Set image type, if you want to do it manually, please comment out the following 2 lines
imageData.setImageType(ImageData.ImageType.FLUORESCENCE)


// Create annotation
QP.createFullImageAnnotation(true)
def FullAnnoSeg = hierarchy.getAnnotationObjects().findAll{it.getPathClass()==null}
def Tissue = QP.getPathClass('Tissue')
FullAnnoSeg.findAll{it.setPathClass(Tissue)}
//def tissue_annotations = [QP.getSelectedObject()]
def tissue_annotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tissue")}
if (tissue_annotations.isEmpty()) {
    print("Cellpose, Please select a parent object!")
    return
}

// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'D:\\QuPath\\QuPath projects\\BIF\\yg.bif\\Script\\White_circles\\models\\DAPI_1'
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


// Celltype detection
QP.runObjectClassifier("DoublePos")
def detCells = hierarchy.getDetectionObjects()
def unclassified = detCells.findAll {it.getPathClass()==null}
def Negative = QP.getPathClass('Negative')
unclassified.forEach({
    it.setPathClass(Negative)
    it.setColor(128, 128, 128)
})
println 'Celltype Detection Done!'

QP.selectAnnotations()
QP.createAnnotationsFromPixelClassifier("aSMA_1", 2.0, 0.0)
QP.createAnnotationsFromPixelClassifier("FAP_1", 2.0, 0.0)
QP.createAnnotationsFromPixelClassifier("PanCK_1", 2.0, 200.0)
QP.resetSelection()

//Detection To Annotation
QP.detectionToAnnotationDistances(true)
println 'Detection To Annotation Done!'

//Detection To Detection
QP.detectionCentroidDistances(false)
println 'Detection To Detection Done!'


//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)

