import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import org.slf4j.LoggerFactory
import qupath.ext.stardist.StarDist2D
import qupath.lib.gui.dialogs.Dialogs


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def pathObjects = [QP.getSelectedObject()]
//def pathObjects = hierarchy.getAnnotationObjects()//.findAll{it.getPathClass() == QP.getPathClass("Tissue")}
if (pathObjects.isEmpty()) {
    Dialogs.showErrorMessage("Stardist", "Please select a parent object!")
    return
}
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)


def pathModel = "C:\\StarDist\\dsb2018_heavy_augment.pb"

// Customize how the StarDist detection should be applied
// Here some reasonable default options are specified
def stardist = StarDist2D
        .builder(pathModel)
        .channels("DAPI (DAPI)")            // Extract channel called 'DAPI'
        .normalizePercentiles(1, 99) // Percentile normalization
        .threshold(0.5)              // Probability (detection) threshold
        .pixelSize(originalPixelSize)              // Resolution for detection
        .cellExpansion(10.0)          // Approximate cells based upon nucleus expansion
        .cellConstrainScale(5)     // Constrain cell expansion using nucleus size
        .constrainToParent(false)    // Prevent nuclei/cells expanding beyond any parent annotations (default is true)
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
        .build()

stardist.detectObjects(imageData, pathObjects)
stardist.close() // This can help clean up & regain memory
println('Done!')

// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)
