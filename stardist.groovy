import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.ImageData
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import qupath.ext.stardist.StarDist2D
import qupath.lib.images.servers.ColorTransforms
import qupath.opencv.dnn.DnnTools
import qupath.opencv.ops.ImageOp
import qupath.opencv.tools.OpenCVTools
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.global.opencv_core
import static qupath.lib.scripting.QP.getDetectionObjects
import static qupath.lib.scripting.QP.measurement
import static qupath.lib.scripting.QP.removeObjects
import static qupath.lib.scripting.QP.runObjectClassifier
import qupath.lib.classifiers.PathClassifierTools
import org.slf4j.LoggerFactory


// Defining
def start = new Date()
def STARDIST_DAPI_LOCATION = 'D:/QuPath/StarDist/dsb2018_heavy_augment.pb'
def DAPI_DNN = DnnTools.builder(STARDIST_DAPI_LOCATION).build()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def selectedObjects = QP.getSelectedObjects() // If you want to run the code automatically, comment out this line and use the following line
//def selectedObjects = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tumour Bed")} // Give annotations name you need to work with e.g. 'Tumor Bed'
if (selectedObjects.isEmpty()) {
    Dialogs.showErrorMessage("StarDist", "Please select a parent object!")
    return
}
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)


//// Set image type, if you want to do it manually, please comment out the following 2 lines
//imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
//QP.setColorDeconvolutionStains('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.886 0.449 0.114", "Stain 2" : "DAB", "Values 2" : "0.356 0.519 0.777", "Background" : " 255 255 255"}')


//// Cell segmentation (StartDist)
def model
model = StarDist2D.builder(DAPI_DNN)
        .threshold(0.6)        // Prediction threshold (usually works well around 0.5)
        .channels(
                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 1),
                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 2),
        )
        .preprocess(new AddChannelsOp(),
        )
        .normalizePercentiles(1, 99)    // Percentile normalization
        .pixelSize(originalPixelSize)   // Resolution for detection
//        .simplify(1)            // Control how polygons are 'simplified' to remove unnecessary vertices
//        .cellExpansion(4.0)     // Approximate cells based upon nucleus expansion
        .includeProbability(true)// Include prediction probability as measurement
        .measureShape()                 // Add shape measurements
        .measureIntensity()             // To get the positive cells out
        .constrainToParent(false)    // Prevent nuclei/cells expanding beyond any parent annotations (default is true)
        .createAnnotations()           // Make annotations instead of detections. This ignores cellExpansion
        .doLog()                        // Use this to log a bit more information while running the script
        .build()

model.detectObjects(imageData, selectedObjects)

// Channels preprocess function
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

//// Filter out small and low intensity nuclei (seems doesn't work)
//def min_nuc_area= 3.5 // Remove any nuclei with an area less than or equal to this value
//def nuc_area_measurement= 'Nucleus: Area µm^2' // If cellExpansion(4.0) function wasn't used in StarDist model, please use 'Area µm^2'
////def nuc_area_measurement= 'Area µm^2'
//def toDelete = getDetectionObjects().findAll {measurement(it, nuc_area_measurement) <= min_nuc_area}
//removeObjects(toDelete, true)
//
//// Close DNN to free up VRAM
//DAPI_DNN.getPredictionFunction().net.close()
//println 'Cell segmentation Done!'
//
//
////// CD8+ cell detection
//def threshold=0.21
//def detObjects = hierarchy.getDetectionObjects()
//logger.info("Measuring "+ detObjects.size() + " Objects")
//PathClassifierTools.setIntensityClassifications(detObjects, "DAB: Cell: Mean", threshold) // If cellExpansion(4.0) function wasn't used in StarDist model, please use "DAB: Mean"
//PathClassifierTools.setIntensityClassifications(detObjects, "DAB: Mean", threshold)
//println 'CD8+ cell detection Done!'


//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)


