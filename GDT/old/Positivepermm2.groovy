import org.slf4j.LoggerFactory
import qupath.lib.images.servers.slidescore.SlideScoreImageServer
import qupath.lib.images.servers.slidescore.SlideScoreUploadAnnotationsCommand
import qupath.lib.scripting.QP


def logger = LoggerFactory.getLogger("base script")
def entry = QP.getProjectEntry()
logger.info(entry.getImageName())
SlideScoreImageServer server = QP.getCurrentServer()
//def res = server.getAnswers("N_Positive_Tumorcells", "y.ge@nki.nl")
def res = server.getAnswers("N_Positive_Cells", "y.ge@nki.nl")
def n_pos = 0
res.each {it->
    n_pos = it.value.toInteger()
    print(n_pos)
}

def ta_question = "Tumor_Area(um2)"
def pa_question = "Positive per mm2"
def slideScoreUploadAnnotationsCommand = new SlideScoreUploadAnnotationsCommand()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
// calculate derived information
def pixelcallibration = imageData.getServer().getPixelCalibration()
def annotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tumor")}
double area_um2 = 0.0
annotations.forEach({
    area_um2 += it.getROI().getScaledArea(pixelcallibration.getPixelWidth() as double, pixelcallibration.getPixelHeight() as double)
})
double area_mm2 = area_um2 / 1e6
double pos_per_mm2 = n_pos / area_mm2
logger.info("Tumor Area is " + area_um2 + "um2")
logger.info("Positive per mm2 is " + pos_per_mm2)
slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, pa_question, pos_per_mm2.round().toBigInteger() as String)
slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, ta_question, area_um2.round().toBigInteger() as String)
