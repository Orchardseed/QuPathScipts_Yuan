import groovy.time.TimeCategory
import org.slf4j.LoggerFactory
import qupath.lib.objects.PathObjectTools
import qupath.lib.scripting.QP

def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def logger = LoggerFactory.getLogger("base script")

//// Celltype detection
QP.runObjectClassifier("Celltype_1")    // remember to change the celltype classifier
logger.info('Celltype detection Done!')


// Find all positive cells
def threshold=0.2
def detObjects = hierarchy.getDetectionObjects()
logger.info("Measuring "+ detObjects.size() + " Objects")
// If cellExpansion(4.0) function wasn't used in Cellpose model, please use "DAB: Mean", otherwise "DAB: Nucleus: Mean"s
PathObjectTools.setIntensityClassifications(detObjects, "DAB: Nucleus: Mean", threshold)
logger.info('Positive cell detection Done!')


// Find all positive cells around tumor region and switch to tumor positive
QP.createAnnotationsFromDensityMap("tumor_region", [0: 5.0], "Tumor", "SELECT_NEW") // remember to add tumor_region heatmap
def detAnn = hierarchy.getAnnotationObjects().find{it.getPathClass() == QP.getPathClass("Tumor")}
def detCells = hierarchy.getObjectsForROI(null, detAnn.getROI()).findAll { it.isDetection() }
def Necrosis = detCells.findAll {it.getPathClass()== QP.getPathClass('Necrosis: Positive')}
def Positive = QP.getPathClass('Tumor: Positive')
Necrosis.forEach({
    it.setPathClass(Positive)
})

QP.clearSelectedObjects(true);

//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)


