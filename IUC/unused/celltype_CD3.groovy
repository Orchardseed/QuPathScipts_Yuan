import groovy.time.TimeCategory
import org.slf4j.LoggerFactory
import qupath.lib.objects.PathObjectTools
import qupath.lib.scripting.QP

def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def detObjects = hierarchy.getDetectionObjects()
def logger = LoggerFactory.getLogger("base script")

//// Celltype detection
QP.runObjectClassifier("Celltype_3")
logger.info('Celltype detection Done!')


def threshold=0.2
logger.info("Measuring "+ detObjects.size() + " Objects")
// If cellExpansion(4.0) function wasn't used in Cellpose model, please use "DAB: Mean", otherwise "DAB: Nucleus: Mean"s
PathObjectTools.setIntensityClassifications(detObjects, "DAB: Nucleus: Mean", threshold)
logger.info('Positive cell detection Done!')


def unclassified = detObjects.findAll {it.getPathClass()== QP.getPathClass('Necrosis: Positive')}
def Positive = QP.getPathClass('Tumor: Positive')
unclassified.forEach({
    it.setPathClass(Positive)
})

//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)


