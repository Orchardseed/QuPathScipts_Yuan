import qupath.lib.scripting.QP
import java.nio.file.FileSystems

def proj = QP.getProject()
for (projectImageEntry in proj.getImageList()) {
    def imagename = projectImageEntry.getImageName()
    def annotations = projectImageEntry.readImageData().getHierarchy().getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tumor")}
    proj.getPath().parent.toString()
    def outpth = FileSystems.getDefault().getPath(proj.getPath().parent as String, imagename.substring(0,10)+".geojson")
    print(outpth.toString())
    QP.exportObjectsToGeoJson(annotations, outpth as String, "EXCLUDE_MEASUREMENTS", "FEATURE_COLLECTION")
}