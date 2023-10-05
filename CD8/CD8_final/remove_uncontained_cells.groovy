import java.util.Collections
import qupath.lib.objects.PathDetectionObject
import qupath.lib.roi.RoiTools
import qupath.lib.scripting.QP

def project = QP.getProject()
for (entry in project.getImageList()) {
    def imageData = entry.readImageData()
    def hierarchy = imageData.getHierarchy()

    def annos = hierarchy.getAnnotationObjects()
    def allAnnotationsROI = RoiTools.union(annos.collect{it.getROI()})
    def spotsTotal = (Collection)(hierarchy == null ? Collections.emptyList() : hierarchy.getObjects((Collection)null, PathDetectionObject.class))
    def stuffInside = hierarchy.getObjectsForROI(qupath.lib.objects.PathDetectionObject, allAnnotationsROI)
    def spotsInside = stuffInside.findAll{it.isCell()}

    spotsTotal.removeAll(spotsInside)
    hierarchy.removeObjects(spotsTotal, true)
    entry.saveImageData(imageData)
    println 'Done!'

}


