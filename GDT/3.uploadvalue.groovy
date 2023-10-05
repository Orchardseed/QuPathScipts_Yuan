import qupath.lib.images.servers.slidescore.SlideScoreUploadAnnotationsCommand
import qupath.lib.roi.ShapeSimplifier
import qupath.lib.scripting.QP

boolean SENDLOCATIONS = false
def aa_question = "Analysed Area"
def nn_question = "N_Negative_Cells"
def np_question = "N_Positive_Cells"
def pt_question = "Positive_Tumorcells"
def nt_question = "Negative_Tumorcells"
// derived information
def pp_question = "Percentage Positive"
def pa_question = "Positive per mm2"
def slideScoreUploadAnnotationsCommand = new SlideScoreUploadAnnotationsCommand()

def imageData = QP.getCurrentImageData()
def hierarchy = QP.getCurrentHierarchy()
def detObjects = hierarchy.getDetectionObjects()

//// Send Analysed Area annotation to slidescore
double altitudeThreshold = 1.0
def pathObjects = QP.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tumor")}
pathObjects.each {
    def roi = ShapeSimplifier.simplifyShape(it.getROI(), altitudeThreshold)
    it.setROI(roi)
}
fireHierarchyUpdate()
slideScoreUploadAnnotationsCommand.submitAnnotations(imageData,pathObjects, aa_question)


//// Send Cells to slidescore
BigInteger n_neg = 0
BigInteger n_pos = 0
detObjects.forEach({
    if (it.getPathClass().toString()=='Tumor: Positive'){
        n_pos+=1
    }else if (it.getPathClass().toString()=='Tumor: Negative'){
        n_neg+=1
    }
})
slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, nn_question, n_neg as String)
slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, np_question, n_pos as String)

// calculate derived information
def pixelcallibration = imageData.getServer().getPixelCalibration()
def annotations = hierarchy.getAnnotationObjects()
double area_um2 = 0.0
annotations.forEach({
    area_um2 += it.getROI().getScaledArea(pixelcallibration.getPixelWidth() as double, pixelcallibration.getPixelHeight() as double)
})
double area_mm2 = area_um2/1e6
double pos_per_mm2 = n_pos/area_mm2
double perc_pos = 100*(n_pos as double)/(n_pos as double + n_neg as double)
slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, pp_question, perc_pos.round().toBigInteger() as String)
slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, pa_question, pos_per_mm2.round().toBigInteger() as String)
