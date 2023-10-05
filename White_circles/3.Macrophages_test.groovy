import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.images.ImageData
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import org.slf4j.LoggerFactory


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def pathObjects = QP.getSelectedObjects()
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)


//// Set image type, if you want to do it manually, please comment out the following 2 lines
imageData.setImageType(ImageData.ImageType.FLUORESCENCE)

//// Create annotation
//QP.createSelectAllObject(true)

// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'D:\\QuPath\\QuPath projects\\BIF\\yg.bif\\Script\\White_circles\\models\\DAPI_1'
def cellpose = Cellpose2D.builder(pathModel)
        .channels('DAPI')
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(originalPixelSize)              // Resolution for detection
        .tileSize(1024)                // If your GPU can take it, make larger tiles to process fewer of them. Useful for Omnipose
//        .diameter(12)                // Average diameter of objects in px (at the requested pixel size)
        .cellExpansion(10.0)          // Approximate cells based upon nucleus expansion
//        .cellConstrainScale(5)     // Constrain cell expansion using nucleus size
        .setOverlap(300)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
//        .excludeEdges()                // Clears objects toutching the edge of the image (Not of the QuPath ROI)
//        .maskThreshold(0.2)           // Threshold for the mask detection, defaults to 0.0
//        .flowThreshold(0.5)            // Threshold for the flows, defaults to 0.4
//        .simplify(1)            // Control how polygons are 'simplified' to remove unnecessary vertices
//        .classify("Cells")     // PathClass to give newly created objects
        .constrainToParent(false)    // Prevent nuclei/cells expanding beyond any parent annotations (default is true)
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
//        .createAnnotations()           // Make annotations instead of detections. This ignores cellExpansion
        .useGPU()
        .build()
//cellpose.overlap =300
cellpose.detectObjects(imageData, pathObjects)

cellpose.cellExpansion = 10.0          // Approximate cells based upon nucleus expansion
cellpose.cellConstrainScale = 5     // Constrain cell expansion using nucleus size

println 'Done!'


//// Filter out small and low intensity nuclei (seems doesn't work)
//def min_nuc_area= 3.5 // Remove any nuclei with an area less than or equal to this value
//def nuc_area_measurement= 'Nucleus: Area µm^2' // If cellExpansion(4.0) function wasn't used in StarDist model, please use 'Area µm^2'
////def nuc_area_measurement= 'Area µm^2'
//def toDelete = getDetectionObjects().findAll {measurement(it, nuc_area_measurement) <= min_nuc_area}
//removeObjects(toDelete, true)dddd
//println 'Cell segmentation Done!'


//def Negative = PathClassFactory.getPathClass('Negative')
//def Stain3 = PathClassFactory.getPathClass('a', ColorTools.packRGB(255, 0, 0))
//def Stain4 = PathClassFactory.getPathClass('CD68', ColorTools.packRGB(0, 255, 0))
//def Stain34 = PathClassFactory.getPathClass('a+CD68', ColorTools.packRGB(255, 255, 0))
//
//def Channel3 = 'AF568: Cell: Mean'
//def Channel4 = 'AF488: Cell: Mean'
//def ChannelMean_3 = 1900
//def ChannelMean_4 = 650
//
//
//def detCells = hierarchy.getDetectionObjects()
//ob = new ObservableMeasurementTableData()
//ob.setImageData(imageData, detCells)
//
//for (def cell in detCells) {
//    if (ob.getNumericValue(cell, Channel3) < ChannelMean_3) {
//        if (ob.getNumericValue(cell, Channel4) < ChannelMean_4) {
//            cell.setPathClass(Negative)
//        } else {
//            if (ob.getNumericValue(cell, Channel4) >= ChannelMean_4) {
//                cell.setPathClass(Stain4)
//            }
//        }
//    } else if (ob.getNumericValue(cell, Channel3) >= ChannelMean_3) {
//        if (ob.getNumericValue(cell, Channel4) < ChannelMean_4) {
//            cell.setPathClass(Stain3)
//        } else {
//            if (ob.getNumericValue(cell, Channel4) >= ChannelMean_4) {
//                cell.setPathClass(Stain34)
//            }
//        }
//    }
//}
//
//QP.runObjectClassifier("CD68_CLEC4F_classifier")
//println 'Celltype detection Done!'
//
//
////// Lipids detection
//QP.createDetectionsFromPixelClassifier("Lipids_1", 0.0, 0.0, "SPLIT")
//println 'Done!'


//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)

