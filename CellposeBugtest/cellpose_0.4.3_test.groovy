/////Brightfield cellpose model 1
import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.color.ColorDeconvolutionStains
import qupath.lib.gui.dialogs.Dialogs
import qupath.lib.images.ImageData
import qupath.opencv.ops.ImageOps
import qupath.lib.objects.PathDetectionObject
import qupath.lib.roi.RoiTools
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import qupath.lib.images.servers.ColorTransforms
import qupath.opencv.ops.ImageOp
import qupath.opencv.tools.OpenCVTools
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.global.opencv_core
import static qupath.lib.scripting.QP.getDetectionObjects
import static qupath.lib.scripting.QP.measurement
import static qupath.lib.scripting.QP.removeObjects
import qupath.lib.objects.PathObjectTools
import org.slf4j.LoggerFactory


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
//def pathObjects = [QP.getSelectedObject()]
def pathObjects = hierarchy.getAnnotationObjects().findAll{it.getPathClass() == QP.getPathClass("Tissue Bed Predicted")}
if (pathObjects.isEmpty()) {
    Dialogs.showErrorMessage("Cellpose", "Please select a parent object!")
    return
}
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)

imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_E)

// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'D:\\QuPath\\Cellpose_models\\nucleitorch_0'
def cellpose = Cellpose2D.builder(pathModel)
        .channels(
                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 1),
//                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 2),
        )
        .preprocess(
                new AddChannelsOp()
        )
//        .normalizePercentilesGlobal(0.1, 99.8, 10) // Convenience global percentile normalization. arguments are percentileMin, percentileMax, dowsample.
        .pixelSize(originalPixelSize)              // Resolution for detection
        .tileSize(4000)                // If your GPU can take it, make larger tiles to process fewer of them. Useful for Omnipose
//        .diameter(11)                // Average diameter of objects in px (at the requested pixel size)
        .cellExpansion(6.0)          // Approximate cells based upon nucleus expansion
//        .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
//        .excludeEdges()                // Clears objects toutching the edge of the image (Not of the QuPath ROI)
        .setOverlap(180)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
//        .maskThreshold(0.2)           // Threshold for the mask detection, defaults to 0.0
//        .flowThreshold(0.5)            // Threshold for the flows, defaults to 0.4
//        .simplify(1)            // Control how polygons are 'simplified' to remove unnecessary vertices
        .constrainToParent(false)    // Prevent nuclei/cells expanding beyond any parent annotations (default is true)
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
//        .classify("Negative")     // PathClass to give newly created objects
//        .createAnnotations()           // Make annotations instead of detections. This ignores cellExpansion
//        .useGPU()
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

cellpose.detectObjects(imageData, pathObjects)
logger.info('Cellpose Cell segmentation Done!')


//// Remove uncontained cells
def allAnnotationsROI = RoiTools.union(pathObjects.collect{it.getROI()})
def spotsTotal = (Collection)(hierarchy == null ? Collections.emptyList() : hierarchy.getObjects((Collection)null, PathDetectionObject.class))
def stuffInside = hierarchy.getObjectsForROI(PathDetectionObject, allAnnotationsROI)
def spotsInside = stuffInside.findAll{it.isCell()}
spotsTotal.removeAll(spotsInside)
hierarchy.removeObjects(spotsTotal, true)
logger.info('Remove uncontained cells Done!')



//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)