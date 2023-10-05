/////Brightfield cellpose model
import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.analysis.DistanceTools
import qupath.lib.color.ColorDeconvolutionStains
import qupath.lib.gui.dialogs.Dialogs
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
import static qupath.lib.scripting.QP.getDetectionObjects
import static qupath.lib.scripting.QP.measurement
import qupath.lib.objects.PathObjectTools
import org.slf4j.LoggerFactory


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
//def pathObjects = [QP.getSelectedObject()]
def pathObjects = QP.getTMACoreList().findAll({!it.isMissing()})
if (pathObjects.isEmpty()) {
    Dialogs.showErrorMessage("Cellpose", "Please select a parent object!")
    return
}
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)

////// Set image type, if you want to dos it manually, please comment out the following 2 lines
////imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
////def myStain = ColorDeconvolutionStains.parseColorDeconvolutionStainsArg('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.758 0.553 0.346", "Stain 2" : "DAB", "Values 2" : "0.138 0.378 0.915", "Background" : "255 255 255"}')
////imageData.setColorDeconvolutionStain(myStain)


// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
//def pathModel = 'D:\\QuPath\\Cellpose_models_retrained_Yuan\\cyto_retrained' //cyto_retrained, cyto2torch_0, CP, CPx
def pathModel = 'D:\\QuPath\\QuPath projects\\BIF\\yg.bif\\Script\\HER2\\HER2_1'

def cellpose = Cellpose2D.builder(pathModel)
        .channels(
                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 1),
                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 2),
        )
        .preprocess(new AddChannelsOp(),
        )
        .normalizePercentiles(1, 99) // Percentile normalization
//        .normalizePercentilesGlobal(0.1, 99.8, 10) // Convenience global percentile normalization. arguments are percentileMin, percentileMax, dowsample.
        .pixelSize(originalPixelSize)              // Resolution for detection
        .tileSize(3500)                // If your GPU can take it, make larger tiles to process fewer of them. Useful for Omnipose
//        .diameter(60)                // Average diameter of objects in px (at the requested pixel size)
        .cellExpansion(6.0)          // Approximate cells based upon nucleus expansion
//        .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
//        .excludeEdges()                // Clears objects toutching the edge of the image (Not of the QuPath ROI)
        .setOverlap(180)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
//        .addParameter("exclude_on_edges")      // Discard masks which touch edges of image
//        .maskThreshold(0.2)           // Threshold for the mask detection, defaults to 0.0
//        .flowThreshold(0.5)            // Threshold for the flows, defaults to 0.4
//        .simplify(1)            // Control how polygons are 'simplified' to remove unnecessary vertices
        .constrainToParent(false)    // Prevent nuclei/cells expanding beyond any parent annotations (default is true)
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
//        .classify("Negative")     // PathClass to give newly created objects
//        .createAnnotations()           // Make annotations instead of detections. This ignores cellExpansion
        .build()

//class AddChannelsOp implements ImageOp {
//    @Override
//    public Mat apply(Mat input) {
//        def channels = OpenCVTools.splitChannels(input)
//        if (channels.size() == 1)
//            return input
//        def sum = opencv_core.add(channels[0], channels[1])
//        for (int i = 2; i < channels.size(); i++)
//            sum = opencv_core.add(sum, channels[i])
//        return sum.asMat()
//    }
//}

//1
class AddChannelsOp implements ImageOp {
    @Override
    public Mat apply(Mat input) {
        def channels = OpenCVTools.splitChannels(input)
        if (channels.size() == 1)
            return input
        def sum = opencv_core.add(channels[0], channels[1])
        def dest = new Mat()
        OpenCVTools.mergeChannels([channels[0],sum.asMat()], dest)
        return dest
    }
}


cellpose.detectObjects(imageData, pathObjects)
logger.info('Cellpose Cell segmentation Done!')


// Celltype detection
QP.runObjectClassifier("Celltype_2")
println 'Celltype Detection Done!'


////// Remove uncontained cells
//def allAnnotationsROI = RoiTools.union(pathObjects.collect{it.getROI()})
//def spotsTotal = (Collection)(hierarchy == null ? Collections.emptyList() : hierarchy.getObjects((Collection)null, PathDetectionObject.class))
//def stuffInside = hierarchy.getObjectsForROI(PathDetectionObject, allAnnotationsROI)
//def spotsInside = stuffInside.findAll{it.isCell()}
//spotsTotal.removeAll(spotsInside)
//hierarchy.removeObjects(spotsTotal, true)
//logger.info('Remove uncontained cells Done!')


////// Filter out small and low intensity nuclei
//def min_nuc_area= 3.5 // Remove any nuclei with an area less than or equal to this value
//def min_OD_value= 0.05 // Remove any nuclei with an area less than or equal to this value
//
//// If cellExpansion(4.0) function wasn't used in StarDist model, please use 'Area µm^2'
////def nuc_area_measurement= 'Nucleus: Area µm^2'
//def nuc_area_measurement= 'Area µm^2'
//def nuc_H_measurement= 'Hematoxylin: Nucleus: Mean'
//def nuc_DAB_measurement= 'DAB: Nucleus: Mean'
//def toDeleteHOD = getDetectionObjects().findAll {measurement(it, nuc_H_measurement) <= min_OD_value}
//def toDeleteDABOD = toDeleteHOD.findAll {measurement(it, nuc_DAB_measurement) <= min_OD_value}
//QP.removeObjects(toDeleteDABOD, true)
//def toDeleteSize = getDetectionObjects().findAll {measurement(it, nuc_area_measurement) <= min_nuc_area}
//QP.removeObjects(toDeleteSize, true)
//logger.info('Remove low intensity nuclei Done!')


//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)

