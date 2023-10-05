/////Brightfield cellpose model 1
import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.images.ImageData
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import qupath.lib.images.servers.ColorTransforms
import qupath.opencv.ops.ImageOp
import qupath.opencv.tools.OpenCVTools
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.global.opencv_core
import org.slf4j.LoggerFactory

// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)


// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'D:\\QuPath\\Cellpose_models\\nucleitorch_0'
def cellpose = Cellpose2D.builder(pathModel)
        .channels(
                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 1),
//                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 2),
        )
//        .preprocess(
//                new AddChannelsOp(),
//        )
        .normalizePercentiles(1, 99) // Percentile normalization
//        .normalizePercentilesGlobal(0.1, 99.8, 10) // Convenience global percentile normalization. arguments are percentileMin, percentileMax, dowsample.
        .pixelSize(originalPixelSize)              // Resolution for detection
        .epochs(1000)              // Optional: will default to 500
        .learningRate(0.2)        // Optional: Will default to 0.2
        .batchSize(16)             // Optional: Will default to 8
        .modelDirectory(new File("D:\\QuPath\\QuPath projects\\BIF\\yg.bif\\Script\\XC")) // Optional place to store resulting model. Will default to QuPath project root, and make a 'models' folder
        .build()

//0
class AddChannelsOp implements ImageOp {
    @Override
    public Mat apply(Mat input) {
        def channels = OpenCVTools.splitChannels(input)
        if (channels.size() == 1)
            return input
        def sum = opencv_core.add(channels[0], channels[1])
        for (int i = 2; i < channels.size(); i++)
            sum = opencv_core.add(sum, channels[i])
        return sum.asMat()
    }
}

////1
//class AddChannelsOp implements ImageOp {
//    @Override
//    public Mat apply(Mat input) {
//        def channels = OpenCVTools.splitChannels(input)
//        if (channels.size() == 1)
//            return input
//        def sum = opencv_core.add(channels[0], channels[1])
//        def dest = new Mat()
//        OpenCVTools.mergeChannels([channels[0],sum.asMat()], dest)
//        return dest
//    }
//}


def resultModel = cellpose.train()

// Pick up results to see how the training was performed
println "Model Saved under "
println resultModel.getAbsolutePath().toString()

// You can get a ResultsTable of the training.
def results = cellpose.getTrainingResults()
results.show("Training Results")

// You can get a results table with the QC results to visualize
def qcResults = cellpose.getQCResults()
qcResults.show("QC Results")

// Finally you have access to a very simple graph
cellpose.showTrainingGraph()
//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)