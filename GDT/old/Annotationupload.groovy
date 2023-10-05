import qupath.lib.scripting.QP
import qupath.lib.roi.ShapeSimplifier
import qupath.lib.images.servers.slidescore.SlideScoreUploadAnnotationsCommand

def imageData = QP.getCurrentImageData()

double altitudeThreshold = 1.0
def pathObjects = QP.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tumor")}
    pathObjects.each {
        def roi = ShapeSimplifier.simplifyShape(it.getROI(), altitudeThreshold)
        it.setROI(roi)
    }
    fireHierarchyUpdate()
def aa_question = "Analysed Area"
def slideScoreUploadAnnotationsCommand = new SlideScoreUploadAnnotationsCommand()
slideScoreUploadAnnotationsCommand.submitAnnotations(imageData,pathObjects, aa_question)

    
    