import groovy.time.TimeCategory
import org.slf4j.LoggerFactory
import qupath.lib.images.servers.slidescore.SlideScoreUploadAnnotationsCommand
import qupath.lib.scripting.QP


def start = new Date()
boolean SENDLOCATIONS = false
def pp_question = "Percentage Positive"
def slideScoreUploadAnnotationsCommand = new SlideScoreUploadAnnotationsCommand()

def entry = QP.getProjectEntry()
def logger = LoggerFactory.getLogger("base script")
logger.info(entry.getImageName())
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def Tumorannotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tumor")}
QP.selectObjects(Tumorannotations)
//QP.mergeSelectedAnnotations()
QP.createAnnotationsFromPixelClassifier("positivepixel", 0, 0)
QP.createAnnotationsFromPixelClassifier("negativepixel", 0, 0)


def pixelcallibration = imageData.getServer().getPixelCalibration()
def Negativeannotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Negative")}
double Negativearea_um2 = 0.0
Negativeannotations.forEach({
    Negativearea_um2 += it.getROI().getScaledArea(pixelcallibration.getPixelWidth() as double, pixelcallibration.getPixelHeight() as double)
})

def Positiveannotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Positive")}
double Positivearea_um2 = 0.0
Positiveannotations.forEach({
    Positivearea_um2 += it.getROI().getScaledArea(pixelcallibration.getPixelWidth() as double, pixelcallibration.getPixelHeight() as double)
})
double perc_pos = 100*(Positivearea_um2 as double)/(Negativearea_um2 as double)

logger.info("Positive Area is " + Positivearea_um2 + "um2")
logger.info("Negative Area is " + Negativearea_um2 + "um2")
logger.info("Percentage Positive is " + perc_pos + "%")

//slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, pp_question, perc_pos.round().toBigInteger() as String)
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus( stop, start )
println(td)

