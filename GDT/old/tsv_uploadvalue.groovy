import qupath.lib.images.servers.slidescore.SlideScoreUploadAnnotationsCommand
import qupath.lib.scripting.QP
import org.slf4j.LoggerFactory

// I put the .tsv files in the root of the project
def logger = LoggerFactory.getLogger("upload_data_tsv")

def nn_question = "N_Negative_Tumorcells"
def np_question = "N_Positive_Tumorcells"
def pp_question = "Percentage Positive"
def ta_question = "Tumor_Area(um2)"
def pa_question = "Positive per mm2"
BigInteger n_neg = 0
BigInteger n_pos = 0
double perc_pos = 0.0
def slideScoreUploadAnnotationsCommand = new SlideScoreUploadAnnotationsCommand()

// locate file
def project = QP.getProject()
def location = project.getPath()
def file = new File(location.parent.toString(), 'measurements_0.15.tsv')
def tsvdata = file.readLines()*.split('\t')
project.getImageList().forEach({imagentry->
    logger.info(imagentry.getImageName())
    tsvdata.forEach({tsv_info->
        if (imagentry.getImageName()==tsv_info[0] && tsv_info.size()==12){
            n_neg = tsv_info[7].toInteger()
            n_pos = tsv_info[8].toInteger()
            logger.info("Found it ; n_neg = "+n_neg + " ; n_pos = "+n_pos)
            def imageData = imagentry.readImageData()
            slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, nn_question, n_neg as String)
            slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, np_question, n_pos as String)
            perc_pos = 100*(n_pos as double)/(n_pos as double + n_neg as double)
            slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, pp_question, perc_pos.round().toBigInteger() as String)
        }
    })
})
