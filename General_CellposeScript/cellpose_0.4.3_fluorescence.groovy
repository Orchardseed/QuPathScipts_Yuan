/////Fluorescence cellpose model
import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.images.ImageData
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.objects.PathDetectionObject
import qupath.lib.roi.RoiTools
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import org.slf4j.LoggerFactory


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def pathObjects = [QP.getSelectedObject()]
//def pathObjects = hierarchy.getAnnotationObjects()//.findAll{it.getPathClass() == QP.getPathClass("Tissue Bed Predicted")}
if (pathObjects.isEmpty()) {
    Dialogs.showErrorMessage("Cellpose", "Please select a parent object!")
    return
}
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)

//imageData.setImageType(ImageData.ImageType.FLUORESCENCE)

// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'path\\to\\your\\model'
def cellpose = Cellpose2D.builder(pathModel)
        .channels('DAPI')
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(originalPixelSize)              // Resolution for detection
        .tileSize(4000)                // If your GPU can take it, make larger tiles to process fewer of them. Useful for Omnipose
//        .diameter(30)                // Average diameter of objects in px (at the requested pixel size)
        .cellExpansion(10.0)          // Approximate cells based upon nucleus expansion
        .cellConstrainScale(5)     // Constrain cell expansion using nucleus size
        .setOverlap(250)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
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

cellpose.detectObjects(imageData, pathObjects)
println 'Cell segmentation Done!'


//// Filter out small and low intensity nuclei
def min_nuc_area= 5 // Remove any nuclei with an area less than or equal to this value
// If cellExpansion(4.0) function wasn't used in StarDist model, please use 'Area µm^2'
def nuc_area_measurement= 'Nucleus: Area µm^2'
//def nuc_area_measurement= 'Area µm^2'
def toDelete = hierarchy.getDetectionObjects().findAll {QP.measurement(it, nuc_area_measurement) <= min_nuc_area}
QP.removeObjects(toDelete, true)
logger.info('Remove low intensity nuclei Done!')


//// Remove uncontained cells
def allAnnotationsROI = RoiTools.union(pathObjects.collect{it.getROI()})
def spotsTotal = (Collection)(hierarchy == null ? Collections.emptyList() : hierarchy.getObjects((Collection)null, PathDetectionObject.class))
def stuffInside = hierarchy.getObjectsForROI(PathDetectionObject, allAnnotationsROI)
def spotsInside = stuffInside.findAll{it.isCell()}
spotsTotal.removeAll(spotsInside)
hierarchy.removeObjects(spotsTotal, true)
logger.info('Remove uncontained cells Done!')


// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)
