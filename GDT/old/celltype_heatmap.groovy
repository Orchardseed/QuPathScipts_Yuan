import qupath.lib.scripting.QP

def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()

QP.runObjectClassifier("Celltype")
def Tissueannotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tissue Bed Predicted")}
QP.selectObjects(Tissueannotations)
QP.createAnnotationsFromDensityMap(imageData,"tumor_region", [0: 5.0], "Tumor")

def Tumorannotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tumor")}
QP.selectObjects(Tumorannotations)
runPlugin('qupath.lib.plugins.objects.RefineAnnotationsPlugin', '{"minFragmentSizeMicrons":50000.0,"maxHoleSizeMicrons":4500.0}')
runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')

print('Done')

