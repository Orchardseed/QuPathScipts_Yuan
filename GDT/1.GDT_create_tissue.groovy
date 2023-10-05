import qupath.lib.scripting.QP
import groovy.time.TimeCategory

// Run detection for the selected objects
def start = new Date()

def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
QP.createAnnotationsFromPixelClassifier("Tissue_1", 200000.0, 100000.0, "SELECT_NEW")
QP.createAnnotationsFromPixelClassifier("Artefacts_2", 200000.0, 2500.0, "SPLIT")
print('Done')
QP.clearSelectedObjects(true)
print('Done')

QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)

