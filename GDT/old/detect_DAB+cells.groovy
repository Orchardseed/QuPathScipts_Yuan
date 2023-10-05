/////Positive cells detection

import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import qupath.lib.objects.PathObjectTools
import qupath.lib.classifiers.PathClassifierTools
import org.slf4j.LoggerFactory


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def logger = LoggerFactory.getLogger("base script")


////// CD8+ cell detection
def threshold=0.20

def allObjects = hierarchy.getDetectionObjects()//.findAll{it.getPathClass() == QP.getPathClass("Tumor")}
//print(allObjects)
logger.info("Measuring "+ allObjects.size() + " Objects")
// If cellExpansion(6.0) function wasn't used in Cellpose model, please use "DAB: Mean"
//PathObjectTools.setIntensityClassifications(detObjects, "DAB: Nucleus: Mean", threshold)
PathClassifierTools.setIntensityClassifications(allObjects, "DAB: Nucleus: Mean", threshold)
println 'CD8+ cell detection Done!'

//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)

