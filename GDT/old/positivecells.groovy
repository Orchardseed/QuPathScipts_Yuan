import qupath.lib.scripting.QP
import qupath.lib.objects.PathObjectTools
import org.slf4j.LoggerFactory


def logger = LoggerFactory.getLogger("base script")
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()

//// CD8+ cell detection
def threshold=0.15
def detObjects = hierarchy.getDetectionObjects()
logger.info("Measuring "+ detObjects.size() + " Objects")
// If cellExpansion(4.0) function wasn't used in StarDist model, please use "DAB: Mean"
PathObjectTools.setIntensityClassifications(detObjects, "DAB: Nucleus: Mean", threshold)
println 'CD8+ cell detection Done!'


