import qupath.lib.color.ColorDeconvolutionStains
import qupath.lib.images.ImageData
import qupath.lib.objects.PathObject
import qupath.lib.plugins.objects.SmoothFeaturesPlugin
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



//// Defining
def start = new Date()

def STARDIST_HE_LOCATION = 'D:/QuPath/StarDist/he_heavy_augment.pb'
def STARDIST_DAPI_LOCATION = 'D:/QuPath/StarDist/dsb2018_heavy_augment.pb'
def HE_DNN = DnnTools.builder(STARDIST_HE_LOCATION).build()
def DAPI_DNN = DnnTools.builder(STARDIST_DAPI_LOCATION).build()
def DO_HE = false    // Only use false, which is DAPI model
def project = QP.getProject()


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

for (entry in project.getImageList()) {
    print(entry)
    def imageData = entry.readImageData()
    def hierarchy = imageData.getHierarchy()
    def tissuebed_annotations = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tissue Bed Predicted")}
    if (tissuebed_annotations.isEmpty()) {
        print("StarDist, Please select a parent object!")
        return
    }
    def server = imageData.getServer()
    def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
    def logger = LoggerFactory.getLogger("base script")
    logger.info("The pixelsize is "+originalPixelSize)
//    print(originalPixelSize)

    imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
    def myStain = ColorDeconvolutionStains.parseColorDeconvolutionStainsArg('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.81349098 0.57214624 0.10431251", "Stain 2" : "DAB", "Values 2" : "0.1675769  0.3998422  0.90113495", "Background" : "255 255 255"}')
    imageData.setColorDeconvolutionStains(myStain)

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

    model.detectObjects(imageData,tissuebed_annotations)

    // Filter out small and low intensity nuclei (seems doesn't work)
    def min_nuc_area= 3.5 //remove any nuclei with an area less than or equal to this value
    def nuc_area_measurement= 'Nucleus: Area µm^2'  // Sometimes 'Area µm^2'
    def toDelete = hierarchy.getDetectionObjects().findAll {measurement(it, nuc_area_measurement) <= min_nuc_area}
    hierarchy.removeObjects(toDelete, true)

    //// Close DNN to free up VRAM
    HE_DNN.getPredictionFunction().net.close()
    DAPI_DNN.getPredictionFunction().net.close()
    println 'Cell segmentation Done!'


    //// Add smoothed features
    List<PathObject> detected_cells = []
    tissuebed_annotations.forEach({detected_cells.addAll(it.getChildObjects())})
    if (detected_cells.isEmpty()) {
        entry.saveImageData(imageData)
        print("SmoothMeasurements, No Cells")
        return
    }
    def annotationMeasurements = detected_cells[0].getMeasurementList().getMeasurementNames()
    SmoothFeaturesPlugin.smoothMeasurements(detected_cells, annotationMeasurements, 25, '25', false, false)
    SmoothFeaturesPlugin.smoothMeasurements(detected_cells, annotationMeasurements, 50, '50', false, false)
    SmoothFeaturesPlugin.smoothMeasurements(detected_cells, annotationMeasurements, 75, '75', false, false)


    //// Ki67+ detection
    def threshold=0.2
    def detObjects = hierarchy.getDetectionObjects()
    logger.info("Measuring "+ detObjects.size() + " Objects")
    PathClassifierTools.setIntensityClassifications(detObjects, "DAB: Nucleus: Mean", threshold)
    println 'Ki67+ cell detection Done!'


    //// Cell classification
    QP.runObjectClassifier(imageData,'Celltypes classifier_final')
    println 'Cell classification Done!'


    //// Tumor bed detection ("Hotspotname", Cell number, bedtype)
    //// Doesn't work in selection annotation and loop!!! need to be solved
    QP.createAnnotationsFromDensityMap(imageData,"Tumor_bed_area_250", [0: 50.0], "Tumor Bed Predicted")
    println 'Tumor bed detection Done!'


    //// Ki67+ Tumor Cells Hotspot detection ("Hotspotname", no idea, Num Hotspots, Min object count, Density peaks only, Delete existing hotspots)
    QP.findDensityMapHotspots(imageData,"Ki67+ Tumor Cells Hotspot", 0, 6, 250.000000, true, false)
    println 'Ki67+ Tumor Cells Hotspot detection Done!'


    //// Save the data
    entry.saveImageData(imageData)
}


//clearDetections()
stop = new Date()
td = TimeCategory.minus( stop, start )
println(td)

