


///////train the cellpose model
//
//import qupath.ext.biop.cellpose.Cellpose2D
//
//def cellpose = Cellpose2D.builder("cyto") // Can choose "None" if you want to train from scratch
//        .cellposechannels(channel5)  // or use work with .cellposeChannels( channel1, channel2 ) and follow the cellpose way
//        .preprocess(ImageOps.Filters.gaussianBlur(1)) // Optional preprocessing QuPath Ops
//        .epochs(500)              // Optional: will default to 500
//        .learningRate(0.2)        // Optional: Will default to 0.2
//        .batchSize(8)             // Optional: Will default to 8
//        .useGPU()                 // Optional: Use the GPU if configured, defaults to CPU only
//        .modelDirectory(new File("D:/QuPath/QuPath projects/BIF/yg.bif")) // Optional place to store resulting model. Will default to QuPath project root, and make a 'models' folder
//        .build()
//
//def resultModel = cellpose.train()
//
//// Pick up results to see how the training was performed
//println "Model Saved under "
//println resultModel.getAbsolutePath().toString()
//
//// You can get a ResultsTable of the training.
//def results = cellpose.getTrainingResults()
//results.show("Training Results")
//
//// Finally you have access to a very simple graph
//cellpose.showTrainingGraph()




///////predict the cellpose model
//
//import qupath.ext.biop.cellpose.Cellpose2D
//
//// Specify the model name (cyto, nuc, cyto2, omni_bact or a path to your custom model)
//def pathModel = 'cyto2'
//def cellpose = Cellpose2D.builder( pathModel )
//        .pixelSize( 0.5 )              // Resolution for detection
//        .channels( 'Channel 1' )            // Select detection channel(s)
////        .preprocess( ImageOps.Filters.median(1) )                // List of preprocessing ImageOps to run on the images before exporting them
////        .tileSize(2048)                // If your GPU can take it, make larger tiles to process fewer of them. Useful for Omnipose
////        .cellposeChannels(1,2)         // Overwrites the logic of this plugin with these two values. These will be sent directly to --chan and --chan2
////        .maskThreshold(-0.2)           // Threshold for the mask detection, defaults to 0.0
////        .flowThreshold(0.5)            // Threshold for the flows, defaults to 0.4
////        .diameter(0)                   // Median object diameter. Set to 0.0 for the `bact_omni` model or for automatic computation
////        .setOverlap(60)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
////        .invert()                      // Have cellpose invert the image
////        .useOmnipose()                 // Add the --omni flag to use the omnipose segmentation model
////        .excludeEdges()                // Clears objects toutching the edge of the image (Not of the QuPath ROI)
////        .clusterDBSCAN()               // Use DBSCAN clustering to avoir over-segmenting long object
//        .cellExpansion(5.0)            // Approximate cells based upon nucleus expansion
////        .cellConstrainScale(1.5)       // Constrain cell expansion using nucleus size
////        .classify("My Detections")     // PathClass to give newly created objects
//        .measureShape()                // Add shape measurements
//        .measureIntensity()            // Add cell measurements (in all compartments)
////        .createAnnotations()           // Make annotations instead of detections. This ignores cellExpansion
//        .useGPU()                      // Optional: Use the GPU if configured, defaults to CPU only
//        .build()
//
//// Run detection for the selected objects
//def imageData = getCurrentImageData()
//def pathObjects = getSelectedObjects()
//if (pathObjects.isEmpty()) {
//    Dialogs.showErrorMessage("Cellpose", "Please select a parent object!")
//    return
//}
//cellpose.detectObjects(imageData, pathObjects)
//println 'Done!'





///////Fluo cellpose model
//import qupath.ext.biop.cellpose.Cellpose2D
//
//// Run detection for the selected objects
//def imageData = getCurrentImageData()
//def pathObjects = getSelectedObjects()
//if (pathObjects.isEmpty()) {
//    Dialogs.showErrorMessage("Cellpose", "Please select a parent object!")
//    return
//}
//// Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
//def pathModel = 'C:/Users/y.ge/.cellpose/models/cytotorch_0'
//
//def cellpose = Cellpose2D.builder(pathModel)
////        .probabilityThreshold(0.5)   // Probability (detection) threshold
//        .channels('Channel 1')            // Select detection channel(s)
//        .normalizePercentiles(1, 99) // Percentile normalization
//        .pixelSize(0.325)              // Resolution for detection
//        .diameter(11.6)                // Average diameter of objects in px (at the requested pixel sie)
////        .cellExpansion(5.0)          // Approximate cells based upon nucleus expansion
//        .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
//        .measureShape()              // Add shape measurements
//        .measureIntensity()          // Add cell measurements (in all compartments)
//        .useGPU()
//        .build()
//

//cellpose.detectObjects(imageData, pathObjects)
//println 'Done!'





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
import static qupath.lib.scripting.QP.getDetectionObjects
import static qupath.lib.scripting.QP.measurement
import static qupath.lib.scripting.QP.removeObjects
import qupath.lib.classifiers.PathClassifierTools
import org.slf4j.LoggerFactory


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def pathObjects = QP.getSelectedObjects()
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)


//// Set image type, if you want to do it manually, please comment out the following 2 lines
imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
QP.setColorDeconvolutionStains('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.886 0.449 0.114", "Stain 2" : "DAB", "Values 2" : "0.356 0.519 0.777", "Background" : " 255 255 255"}')

// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'C:/Users/y.ge/.cellpose/models/cyto2torch_0'
def cellpose = Cellpose2D.builder(pathModel)
        .channels(
                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 1),
                ColorTransforms.createColorDeconvolvedChannel(imageData.getColorDeconvolutionStains(), 2),
        )
        .preprocess(new AddChannelsOp(),
        )
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(originalPixelSize)              // Resolution for detection
//        .diameter(11)                // Average diameter of objects in px (at the requested pixel size)
//        .cellExpansion(4.0)          // Approximate cells based upon nucleus expansion
//        .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
        .setOverlap(60)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
//        .maskThreshold(0.2)           // Threshold for the mask detection, defaults to 0.0
//        .flowThreshold(0.5)            // Threshold for the flows, defaults to 0.4
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
        .useGPU()
        .build()

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
println 'Done!'

// Filter out small and low intensity nuclei (seems doesn't work)
def min_nuc_area= 3.5 // Remove any nuclei with an area less than or equal to this value
//def nuc_area_measurement= 'Nucleus: Area µm^2' // If cellExpansion(4.0) function wasn't used in StarDist model, please use 'Area µm^2'
def nuc_area_measurement= 'Area µm^2'
def toDelete = getDetectionObjects().findAll {measurement(it, nuc_area_measurement) <= min_nuc_area}
removeObjects(toDelete, true)
println 'Cell segmentation Done!'


//// CD8+ cell detection
def threshold=0.2
def detObjects = hierarchy.getDetectionObjects()
logger.info("Measuring "+ detObjects.size() + " Objects")
//PathClassifierTools.setIntensityClassifications(detObjects, "DAB: Nucleus: Mean", threshold) // If cellExpansion(4.0) function wasn't used in StarDist model, please use "DAB: Mean"
PathClassifierTools.setIntensityClassifications(detObjects, "DAB: Mean", threshold)
println 'CD8+ cell detection Done!'

//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)






/////////Brightfield cellpose model 2
//import qupath.ext.biop.cellpose.Cellpose2D
//import org.slf4j.LoggerFactory
//
//def imageData = getCurrentImageData()
//def pathObjects = getSelectedObjects()
//if (pathObjects.isEmpty()) {
//    Dialogs.showErrorMessage("Cellpose", "Please select a parent object!")
//    return
//}
//def server = imageData.getServer()
//def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
//def logger = LoggerFactory.getLogger("base script")
//logger.info("The pixelsize is "+originalPixelSize)
////print(originalPixelSize)
//
//imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
//setColorDeconvolutionStains('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.886 0.449 0.114", "Stain 2" : "DAB", "Values 2" : "0.356 0.519 0.777", "Background" : " 255 255 255"}')
//// Specify the model name (cyto, nuc, cyto2, omni_bact or a path to your custom model)
//
//def pathModel = 'C:/Users/y.ge/.cellpose/models/cyto2torch_0'
//def stains = getCurrentImageData().getColorDeconvolutionStains() //will be null if IF
//def cellpose = Cellpose2D.builder(pathModel)
//        .pixelSize(originalPixelSize)              // Resolution for detection
//        .preprocess(
//                ImageOps.Channels.deconvolve(stains),
//                ImageOps.Channels.extract(0), //Typically Hx is 0, DAB is 1
//        )
////        .normalizePercentiles(1, 99) // Percentile normalization
////        .preprocess( ImageOps.Filters.median(1) )  // List of preprocessing ImageOps to run on the images before exporting them for prediction
////        .tileSize(2048*2)                // If your GPU can take it, make larger tiles to process fewer of them. Useful for Omnipose
////        .cellposeChannels(1,2)         // Overwrites the logic of this plugin with these two values. These will be sent directly to --chan and --chan2
////        .maskThreshold(-0.2)           // Threshold for the mask detection, defaults to 0.0
////        .flowThreshold(0.5)            // Threshold for the flows, defaults to 0.4
//        .setOverlap(60)                // Overlap between tiles (in pixels) that the QuPath Cellpose Extension will extract. Defaults to 2x the diameter or 60 px if the diameter is set to 0
//        .diameter(30)                   // Median object diameter. Set to 0.0 for the `bact_omni` model or for automatic computation
////        .invert()                      // Have cellpose invert the image
////        .useOmnipose()                 // Add the --omni flag to use the omnipose segmentation model
////        .excludeEdges()                // Clears objects toutching the edge of the image (Not of the QuPath ROI)
////        .clusterDBSCAN()               // Use DBSCAN clustering to avoir over-segmenting long object
////        .cellExpansion(2.0)            // Approximate cells based upon nucleus expansion
////        .cellConstrainScale(1.5)       // Constrain cell expansion using nucleus size
////        .classify("My Detections")     // PathClass to give newly created objects
//        .measureShape()                // Add shape measurements
//        .measureIntensity()            // Add cell measurements (in all compartments)
////        .createAnnotations()           // Make annotations instead of detections. This ignores cellExpansion()
//        .useGPU()                      // Optional: Use the GPU if configured, defaults to CPU only
//        .build()
//
//// Run detection for the selected objects
//cellpose.detectObjects(imageData, pathObjects)
//println 'Done!'




//// merge every object
//import org.locationtech.jts.geom.util.GeometryCombiner;
//import org.locationtech.jts.operation.union.UnaryUnionOp
//import qupath.lib.roi.GeometryTools
//import qupath.lib.roi.RoiTools
//import qupath.lib.objects.PathObjects
//
//def islets= getDetectionObjects()
//def geos=islets.collect{it.getROI().getGeometry()}
//def combined=GeometryCombiner.combine(geos)
//def merged= UnaryUnionOp.union(combined)
//def mergedRois=GeometryTools.geometryToROI(merged, islets[0].getROI().getImagePlane())
//def splitRois=RoiTools.splitROI(mergedRois)
//
//def newObjs=[]
//splitRois.each{
//    newObjs << PathObjects.createDetectionObject(it,getPathClass("Region"))
//}
//addObjects(newObjs)
//removeObjects(islets,true)






