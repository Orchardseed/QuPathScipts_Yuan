/////Brightfield cellpose model 1
import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.color.ColorDeconvolutionStains
import qupath.lib.images.ImageData
import qupath.lib.objects.PathDetectionObject
import qupath.lib.roi.RoiTools
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import qupath.lib.images.servers.ColorTransforms
import qupath.opencv.ops.ImageOp
import qupath.opencv.tools.OpenCVTools
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.global.opencv_core
import static qupath.lib.scripting.QP.measurement
import qupath.lib.classifiers.PathClassifierTools
import org.slf4j.LoggerFactory



// Run detection for the selected objects
def start = new Date()
def project = QP.getProject()

// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'D:\\QuPath\\QuPath projects\\BIF\\yg.bif\\Script\\CD8\\test3_6_3'


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
    def pathObjects = hierarchy.getAnnotationObjects()
    if (pathObjects.isEmpty()) {
        print("Cellpose, Please select a parent object!")
        continue
    }
    def server = imageData.getServer()
    def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
    def logger = LoggerFactory.getLogger("base script")
    logger.info("The pixelsize is " + originalPixelSize)
    //print(originalPixelSize)

    //// Set image type, if you want to do it manually, please comment out the following 2 lines
    imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
    def myStain = ColorDeconvolutionStains.parseColorDeconvolutionStainsArg('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.886 0.449 0.114", "Stain 2" : "DAB", "Values 2" : "0.356 0.519 0.777", "Background" : " 255 255 255"}')
    imageData.setColorDeconvolutionStains(myStain)
    println 'set Image Type Done!'

    ////Cellpsose model
    def cellpose = Cellpose2D.builder(pathModel)
            .channels(
                    ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 1),
                    ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 2),
            )
            .preprocess(new AddChannelsOp(),
            )
            .normalizePercentiles(1, 99) // Percentile normalization
            .pixelSize(originalPixelSize)              // Resolution for detection
            .tileSize(2048)                // If your GPU can take it, make larger tiles to process fewer of them. Useful for Omnipose
    //        .diameter(11)                // Average diameter of objects in px (at the requested pixel size)
            .cellExpansion(4.0)          // Approximate cells based upon nucleus expansion
    //        .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
    //        .excludeEdges()                // Clears objects toutching the edge of the image (Not of the QuPath ROI)
            .setOverlap(250)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
    //        .maskThreshold(0.2)           // Threshold for the mask detection, defaults to 0.0
    //        .flowThreshold(0.5)            // Threshold for the flows, defaults to 0.4
    //        .simplify(1)            // Control how polygons are 'simplified' to remove unnecessary vertices
            .constrainToParent(false)    // Prevent nuclei/cells expanding beyond any parent annotations (default is true)
            .measureShape()              // Add shape measurements
            .measureIntensity()          // Add cell measurements (in all compartments)
//            .classify("Cells")     // PathClass to give newly created objects
    //        .createAnnotations()           // Make annotations instead of detections. This ignores cellExpansion
            .useGPU()
            .build()

    cellpose.detectObjects(imageData, pathObjects)
    println 'Cell segmentation Done!'


    // Filter out small and low intensity nuclei (seems doesn't work)
    def min_nuc_area = 3.5 // Remove any nuclei with an area less than or equal to this value
    def nuc_area_measurement = 'Nucleus: Area µm^2'
    // If cellExpansion(4.0) function wasn't used in StarDist model, please use 'Area µm^2'
    //def nuc_area_measurement= 'Area µm^2'
    def toDelete = hierarchy.getDetectionObjects().findAll { measurement(it, nuc_area_measurement) <= min_nuc_area }
    hierarchy.removeObjects(toDelete, true)
    println 'Remove nucleus with an area less than 3.5 Done!'

    // Remove uncontained cells
    def allAnnotationsROI = RoiTools.union(pathObjects.collect{it.getROI()})
    def spotsTotal = (Collection)(hierarchy == null ? Collections.emptyList() : hierarchy.getObjects((Collection)null, PathDetectionObject.class))
    def stuffInside = hierarchy.getObjectsForROI(qupath.lib.objects.PathDetectionObject, allAnnotationsROI)
    def spotsInside = stuffInside.findAll{it.isCell()}
    spotsTotal.removeAll(spotsInside)
    hierarchy.removeObjects(spotsTotal, true)
    println 'Remove uncontained cells Done!'

    //// CD8+ cell detection
    def threshold = 0.20
    def detObjects = hierarchy.getDetectionObjects()
    logger.info("Measuring " + detObjects.size() + " Objects")
    PathClassifierTools.setIntensityClassifications(detObjects, "DAB: Nucleus: Mean", threshold)
    // If cellExpansion(4.0) function wasn't used in StarDist model, please use "DAB: Mean"
    //PathClassifierTools.setIntensityClassifications(detObjects, "DAB: Mean", threshold)
    println 'CD8+ cell detection Done!'

    //// Save
    entry.saveImageData(imageData)
}

stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)

