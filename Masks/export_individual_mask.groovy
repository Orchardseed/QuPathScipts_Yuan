import qupath.lib.images.servers.LabeledImageServer
import qupath.lib.objects.PathObject
import qupath.lib.scripting.QP
import qupath.lib.common.ColorTools
import qupath.lib.common.GeneralTools
import qupath.lib.regions.RegionRequest
import qupath.lib.images.writers.ome.OMEPyramidWriter

def imageData = QP.getCurrentImageData()
// Define output path (relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def pathOutput = QP.buildFilePath(QP.PROJECT_BASE_DIR, 'masks')
mkdirs(pathOutput)

// Define output resolution
double downsample = 1

// Create an ImageServer where the pixels are derived from annotations
def labelServer = new LabeledImageServer.Builder(imageData)
        .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
        .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
        .addLabel('tumor', 1)      // Choose output labels (the order matters!)
        .addLabel('stroma', 2)
        .addLabel('other classes', 3)
        .lineThickness(2)          // Optionally export annotation boundaries with another label
        .setBoundaryLabel('Boundary*', 4) // Define annotation boundary label
        .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
        .build()


QP.getAnnotationObjects().findAll({it.getPathClass() == QP.getPathClass("Layer 1")}).eachWithIndex { PathObject annotation, int i ->
    def region = RegionRequest.createInstance(labelServer.getPath(), downsample, annotation.getROI())
    def outputPath = QP.buildFilePath(pathOutput, 'Region ' + i + '.png')
    QP.writeImageRegion(labelServer, region, outputPath)
}
print('Done')

// Export each region
//int i = 0
//for (annotation in getAnnotationObjects()) {
//    def region = RegionRequest.createInstance(
//            labelServer.getPath(), 1, annotation.getROI())
//    i++
//    def outputPath = QP.buildFilePath(pathOutput, 'Region ' + i + '.png')
//    QP.writeImageRegion(labelServer, region, outputPath)
//}

//def roi = QP.getSelectedObject().getROI()
////def roi = QP.getAnnotationObjects().each{it.getPathClass() == QP.getPathClass("Layer 1")}.getROI()
//def region = RegionRequest.createInstance(labelServer.getPath(), downsample, roi)
//def outputPath = QP.buildFilePath(pathOutput, 'Region ' + i + '.png')
//QP.writeImageRegion(labelServer, region, outputPath)
//def mywriter = new OMEPyramidWriter()
//mywriter.writeImage(labelServer, pathOutput, OMEPyramidWriter.CompressionType.DEFAULT, region)
//print('Done')

