import qupath.lib.objects.PathObjects
import qupath.lib.roi.RoiTools
import qupath.lib.scripting.QP


// Remove all annotations outside of a selected region
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def aSMA = QP.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("aSMA")}
def FAP = QP.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("FAP")}
def aSMA_FAP = QP.getPathClass('aSMA+FAP')
aSMA.each{
    def roia = it.getROI()
    FAP.each{
        def roib = it.getROI()
        def roi = RoiTools.combineROIs(roia, roib, RoiTools.CombineOp.INTERSECT)
        if (!roi.isEmpty()){
            def annotation = PathObjects.createAnnotationObject(roi)
            annotation.setPathClass(aSMA_FAP)
            hierarchy.addObject(annotation)
        }
    }
}



