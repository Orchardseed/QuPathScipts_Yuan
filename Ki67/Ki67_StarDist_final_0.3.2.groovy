import qupath.lib.color.ColorDeconvolutionStains
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
import static qupath.lib.scripting.QP.measurement
import qupath.lib.classifiers.PathClassifierTools
import org.slf4j.LoggerFactory



// Run detection for the selected objects
def start = new Date()
def STARDIST_HE_LOCATION = 'D:/QuPath/StarDist/he_heavy_augment.pb'
def STARDIST_DAPI_LOCATION = 'D:/QuPath/StarDist/dsb2018_heavy_augment.pb'
def HE_DNN = DnnTools.builder(STARDIST_HE_LOCATION).build()
def DAPI_DNN = DnnTools.builder(STARDIST_DAPI_LOCATION).build()
def DO_HE = false    // Only use false, which is DAPI model

QP.createAnnotationsFromPixelClassifier("Tissue Bed 3", 100000.0, 0.0)
def imageData = QP.getCurrentImageData()
//def selectedObjects = QP.getSelectedObjects()
def hierarchy = imageData.getHierarchy()
def selectedObjects = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tissue Bed Predicted")}
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
imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
def myStain = ColorDeconvolutionStains.parseColorDeconvolutionStainsArg('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.81349098 0.57214624 0.10431251", "Stain 2" : "DAB", "Values 2" : "0.1675769  0.3998422  0.90113495", "Background" : "255 255 255"}')
imageData.setColorDeconvolutionStains(myStain)

/// cell segmentation (StartDist)
def model
if (DO_HE){
    model = StarDist2D.builder(HE_DNN)      // Don't use in Ki67
            .threshold(0.5)        // Prediction threshold
            .channels()                     // Select detection channel
            .normalizePercentiles(1, 99)    // Percentile normalization
            .pixelSize(originalPixelSize)   // Resolution for detection
            .simplify(1)            // Control how polygons are 'simplified' to remove unnecessary vertices
            .cellExpansion(2.5)     // Approximate cells based upon nucleus expansion
            .measureIntensity()             // To get the positive cells out
            .doLog()                        // Use this to log a bit more information while running the script
            .build()
}else{
    model = StarDist2D.builder(DAPI_DNN)
            .threshold(0.5)                 // Prediction threshold
            .channels(
                    ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 1),
                    ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 2),
            )
            .preprocess(new AddChannelsOp(),
            )
            .normalizePercentiles(1, 99)    // Percentile normalization
            .pixelSize(originalPixelSize)   // Resolution for detection
            .simplify(1)            // Control how polygons are 'simplified' to remove unnecessary vertices
            .cellExpansion(2.5)     // Approximate cells based upon nucleus expansion
            .includeProbability(true)// Include prediction probability as measurement
            .measureShape()                 // Add shape measurements
            .measureIntensity()             // To get the positive cells out
            .constrainToParent(false)    // Prevent nuclei/cells expanding beyond any parent annotations (default is true)
            .doLog()                        // Use this to log a bit more information while running the script
            .build()
}

model.detectObjects(imageData, selectedObjects)


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

//filter out small and low intensity nuclei (seems doesn't work)
def min_nuc_area= 3.5 //remove any nuclei with an area less than or equal to this value
//def nuc_area_measurement= 'Area µm^2'
def nuc_area_measurement= 'Nucleus: Area µm^2'
def toDelete = QP.getDetectionObjects().findAll {measurement(it, nuc_area_measurement) <= min_nuc_area}
QP.removeObjects(toDelete, true)
logger.info('Remove nucleus with an area less than 3.5 Done!')

//CLOSE DNN TO FREE UP VRAM
HE_DNN.getPredictionFunction().net.close()
DAPI_DNN.getPredictionFunction().net.close()
logger.info('Cell segmentation Done!')


// Add smoothed features
QP.selectObjects(selectedObjects)
QP.runPlugin('qupath.lib.plugins.objects.SmoothFeaturesPlugin', '{"fwhmMicrons": 25.0,  "smoothWithinClasses": false}')
QP.runPlugin('qupath.lib.plugins.objects.SmoothFeaturesPlugin', '{"fwhmMicrons": 50.0,  "smoothWithinClasses": false}')
QP.runPlugin('qupath.lib.plugins.objects.SmoothFeaturesPlugin', '{"fwhmMicrons": 75.0,  "smoothWithinClasses": false}')
logger.info('Smooth Done!')


// Ki67+ detection
def threshold=0.2
def detObjects = hierarchy.getDetectionObjects()
logger.info("Measuring "+ detObjects.size() + " Objects")
PathClassifierTools.setIntensityClassifications(detObjects, "DAB: Nucleus: Mean", threshold)
logger.info('Ki67+ Tumor cell detection Done!')


// cell classification
QP.runObjectClassifier('Celltypes classifier_final')
logger.info('Cell classification Done!')


// Tumor bed detection ("Hotspotname", Cell number, bedtype)
QP.createAnnotationsFromDensityMap("Tumor_bed_area_250", [0: 50.0], "Tumor Bed Predicted")
logger.info('Tumor bed detection Done!')

////
// Ki67+ Tumor Cells Hotspot detection ("Hotspotname", no idea, Num Hotspots, Min object count, Density peaks only, Delete existing hotspots)
def Tumorbed = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tumor Bed Predicted")}
QP.selectObjects(Tumorbed)
QP.findDensityMapHotspots("Ki67+ Tumor Cells Hotspot", 0, 6, 250.000000, true, false)
logger.info('Ki67+ Tumor Cells Hotspot detection Done!')


QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus( stop, start )
println(td)

