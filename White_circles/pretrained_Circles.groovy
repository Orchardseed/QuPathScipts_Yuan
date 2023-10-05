/////Brightfield cellpose model 1
import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.images.ImageData
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import qupath.lib.images.servers.ColorTransforms
import qupath.opencv.ops.ImageOp
import qupath.opencv.tools.OpenCVTools
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.global.opencv_core
import static qupath.lib.scripting.QP.getDetectionObjects
import static qupath.lib.scripting.QP.measurement
import static qupath.lib.scripting.QP.removeObjects
import qupath.lib.classifiers.PathClassifierTools
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

// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'C:\\Users\\49847\\.cellpose\\models\\cyto2torch_0'
def cellpose = Cellpose2D.builder(pathModel)
        .channels('AF647')
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(originalPixelSize)              // Resolution for detection
//        .diameter(3)                // Average diameter of objects in px (at the requested pixel size)1
//        .cellExpansion(4.0)          // Approximate cells based upon nucleus expansion
//        .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
//        .excludeEdges()                // Clears objects toutching the edge of the image (Not of the QuPath ROI)
        .setOverlap(160)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
//        .maskThreshold(0.2)           // Threshold for the mask detection, defaults to 0.0
//        .flowThreshold(0.5)            // Threshold for the flows, defaults to 0.4
//        .simplify(1)            // Control how polygons are 'simplified' to remove unnecessary vertices
        .constrainToParent(false)    // Prevent nuclei/cells expanding beyond any parent annotations (default is true)
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
//        .createAnnotations()           // Make annotations instead of detections. This ignores cellExpansion
        .useGPU()
        .build()

cellpose.detectObjects(imageData, pathObjects)
println 'Done!'

//// Save
//QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)

