//import qupath.lib.common.GeneralTools
//import qupath.lib.scripting.QP
//import static qupath.lib.scripting.QP.clearDetections
//import org.bytedeco.opencv.opencv_core.Mat
//import qupath.opencv.ops.ImageOp
//import qupath.opencv.tools.OpenCVTools



///////////PixelClassifier在Ki67 detection///////////////
//import qupath.lib.scripting.QP
//import org.slf4j.LoggerFactory
//import qupath.lib.classifiers.PathClassifierTools
//import qupath.opencv.ml.pixel.PixelClassifierTools
//
//static def classifyAndThreshold(ImageData imageData, classifier, threshold){
//    def logger = LoggerFactory.getLogger("Classify and Threshold")
//    PixelClassifierTools.classifyDetectionsByCentroid(imageData, classifier)
//    def hierarchy = imageData.getHierarchy()
//    def detObjects = hierarchy.getDetectionObjects()
//    logger.info("Measuring "+ detObjects.size() + " Objects")
//    PathClassifierTools.setIntensityClassifications(detObjects, "DAB: Mean", threshold)
//}
//
//def imageData = QP.getCurrentImageData()
//def threshold=0.2
//def project = QP.getProject()
//def classifiers = project.getPixelClassifiers()
//def classifier = classifiers.get('classifier_shape_features')
//classifyAndThreshold(imageData, classifier, threshold)



///////////////如何找通道////////////////
//
//import qupath.lib.scripting.QP
//// By importing QP I can call `QP.getProject()` or `QP.setImageType` instead of `getProject()` or `setImageType()`.
//// So it means more typing... The reason I do it is because it tells my IDE (IntelliJ) what all these functions mean.
//// For example, I get feedback on what functions return.
//// As you can see, QP.getProject() returns something called `Project<BufferedImage>`
//import qupath.lib.color.ColorDeconvolutionStains
//
//def myProject = QP.getProject()  // Get a reference to the project
//def number_images = myProject.getImageList().size()
//myProject.getImageList().eachWithIndex{ entry, int i ->  // Here we loop over every image in the project
//    def imageData = entry.readImageData()  // Running commands is often done on the imageData
//    imageData.setImageType(imageData.imageType.BRIGHTFIELD_H_DAB)  // We want to set the type of image for each image
//    // This staining vector was found by selecting a region with DAB and Hematoxylin and automatically estimating stain vectors.
//    def allchannels = '{"Name" : "H-DAB estimated", "Stain 1" : "Hematoxylin", -->
//    "Values 1" : "0.84358 0.51753 0.1433", "Stain 2" : "DAB", "Values 2" : "0.36865 0.56061 0.74149", "Background" : " 255 255 255"}'
//    def stains = ColorDeconvolutionStains.parseColorDeconvolutionStainsArg(allchannels)
//    imageData.setColorDeconvolutionStains(stains)
//    entry.saveImageData(imageData)  // Save the imageData (important!)
//    def imageName = entry.getImageName()  // Can be usefull for feedback. Here I just print the name with the index.
//    println(imageName +" ; "+ (1+i).toString() + " of " + number_images.toString()) // (1+i) because i starts at zero.
//}




///////////pixel classifier参考//////////
//
//import qupath.lib.scripting.QP
//import qupath.opencv.ml.pixel.PixelClassifierTools
//import qupath.lib.io.GsonTools
//
//
//// for testing the script I need references to imageData and myProject
//def myProject = QP.getProject()
//def imageData = QP.getCurrentImageData()
//
//// locate the classifier
//def classifiers = myProject.getPixelClassifiers()
//def classifier = classifiers.get('tumor_det_RH_20210930')
//def hierarchy = imageData.getHierarchy()  // contains the hierarchy of objects (TMA cores, annotations, detections)
//
//// make sure there is only a single object, and select that object
//def objects = hierarchy.getAnnotationObjects()
//if (objects.size()>1){
//    println("ERROR! More than one object")
//    return
//}
//
//hierarchy.getSelectionModel().setSelectedObjects([objects[0]], objects[0])  // select one object, and set it as primary
//
//// run the classifier only within that selection
//PixelClassifierTools.createAnnotationsFromPixelClassifier(imageData, classifier, 2000, 2000)
//
//// select the tumor
//objects[0].getChildObjects().each {
//    if (it.getPathClass().toString()=='Tumor'){
//        hierarchy.getSelectionModel().setSelectedObjects([it], it)
//    }
//}
//
//// With the large amount of settings that Positive Cell Detection requires, it is hard to construct the options string:
//// str = "{\"detectionImageBrightfield\": "+mychannel+", \"requestedPixelSizeMicrons\":"+pixsz+ "}" (for just two options)
//// Since the string is JSON, you can also generate all 19 options from a LinkedHasMap like so:
//
//def celldetectionsettings = [
//        "detectionImageBrightfield": "Hematoxylin OD",
//        "requestedPixelSizeMicrons": 0.5,
//        "backgroundRadiusMicrons": 8.0,
//        "medianRadiusMicrons": 0.0,
//        "sigmaMicrons": 1.5,
//
//        "minAreaMicrons": 10.0,
//        "maxAreaMicrons": 400.0,
//        "threshold": 0.1,
//        "maxBackground": 2.0,
//        "watershedPostProcess": true,
//
//        "excludeDAB": false,
//        "cellExpansionMicrons": 5.0,
//        "includeNuclei": true,
//        "smoothBoundaries": true,
//        "makeMeasurements": true,
//
//        "thresholdCompartment": "Nucleus: DAB OD mean",
//        "thresholdPositive1": 0.2,
//        "thresholdPositive2": 0.4,
//        "thresholdPositive3": 0.6000000000000001,
//        "singleThreshold": true]
//
//
//boolean prettyPrint = true
//def gson = GsonTools.getInstance(prettyPrint)
//QP.runPlugin('qupath.imagej.detect.cells.PositiveCellDetection',gson.toJson(celldetectionsettings))
//
//println("FINISH!")




////I wrote this script to change the size of an annotation to a square of 512x512./////
////(actually, make a new annotation and remove the old one)
//
//import qupath.lib.scripting.QP
//import qupath.lib.objects.PathAnnotationObject
//import qupath.lib.roi.ROIs
//
//def ann_sz = [512,512]
//def ann_sz = [1024,1024]
//def myHierarchy = QP.getCurrentHierarchy()
//def myObject = QP.getSelectedObject()
//if (myObject.isAnnotation() == false){
//    println("ERROR: Not an Annotation")
//    return
//}
//
//def myImagePlane = myObject.getROI().getImagePlane()  // create annotation in same z-slice, channel, timepoint
//def myPoints = myObject.getROI().getAllPoints()
//def newPathAnnotationObject = new PathAnnotationObject()
//def newROI = ROIs.createRectangleROI(myPoints[0].x,myPoints[0].y, ann_sz[0], ann_sz[1], myImagePlane)
//
//newPathAnnotationObject.setROI(newROI)
//myHierarchy.removeObject(myObject,false)
//myHierarchy.addPathObject(newPathAnnotationObject)
//myHierarchy.getSelectionModel().setSelectedObjects([newPathAnnotationObject], newPathAnnotationObject)
//myHierarchy.fireHierarchyChangedEvent(newPathAnnotationObject)
//
//println("Finished")




////Simple: send current annotation to Slide Score//////
//import qupath.lib.images.servers.slidescore.SlideScoreUploadAnnotationsCommand
//import qupath.lib.scripting.QP
//
//def slide_score_question = 'Necrosis'   // need to change
//def myObjects = QP.getSelectedObjects()
//def imageData = QP.getCurrentImageData()
//
//def slideScoreUploadAnnotationsCommand = new SlideScoreUploadAnnotationsCommand()
//slideScoreUploadAnnotationsCommand.submitAnnotations(imageData, myObjects, slide_score_question)
////slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, '% Positive Tumorcells (AI algorithm)', "60")
//
//
//
//import qupath.lib.scripting.QP
//import qupath.lib.images.servers.slidescore.SlideScoreUploadAnnotationsCommand
//def imageData = QP.getCurrentImageData()
//def hierarchy = imageData.getHierarchy()
//def objects = hierarchy.getAnnotationObjects()
//def object = objects[0]
//def nPos = 0
//object.getChildObjects().each {if (it.getPathClass().toString()=='Positive'){nPos++}}
//def percentage_positive = 100*nPos/object.getChildObjects().size()
//def slideScoreUploadAnnotationsCommand = new SlideScoreUploadAnnotationsCommand()
//percentage_positive = percentage_positive.round()
//slideScoreUploadAnnotationsCommand.submitSlideAnswer(imageData, '% positive', percentage_positive.toString())




////如何写function 大概这样
//public static Double get_threshold(Channel){}
//



//// run stardist on every image
//def myProject = QP.getProject()  // Get a reference to the project
//def number_images = myProject.getImageList().size()
//myProject.getImageList().eachWithIndex{ entry, int i ->
//    def newimageData = entry.readImageData()
//
//
//    model.detectObjects(imageData, selectedObjects)
//}
//////




//////如何更改 图像类型 和 pixel size
//import qupath.lib.scripting.QP
//QP.setPixelSizeMicrons(0.5,0.5)
//
//setImageType('BRIGHTFIELD_H_DAB')
//setColorDeconvolutionStains('{"Name" : "H-DAB default", "Stain 1" : "Hematoxylin", "Values 1" : "0.65111 0.70119 0.29049",
// "Stain 2" : "DAB", "Values 2" : "0.26917 0.56824 0.77759", "Background" : " 255 255 255"}');
//
//runPlugin('qupath.imagej.detect.cells.PositiveCellDetection', '{"detectionImageBrightfield": "Optical density sum",
// "requestedPixelSizeMicrons": 0.5, "backgroundRadiusMicrons": 8.0,  "medianRadiusMicrons": 0.0,  "sigmaMicrons": 1.5,
// "minAreaMicrons": 10.0, "maxAreaMicrons": 400.0,  "threshold": 0.1,  "maxBackground": 2.0,  "watershedPostProcess": true,
// "excludeDAB": false,  "cellExpansionMicrons": 5.0,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true,
// "thresholdCompartment": "Cell: DAB OD mean",  "thresholdPositive1": 0.4,  "thresholdPositive2": 0.4,
// "thresholdPositive3": 0.6000000000000001,  "singleThreshold": true}');



//clearDetections()


//// 用 //python// 处理通道，变为我们希望的RGB颜色等等
//
//def getstain():
//"""
//    Get default absorptionstains. (copied from QuPath)
//    """
//return {'Hematoxylin': np.array((0.651, 0.701, 0.29), dtype=np.float),
//    'DAB': np.array((0.269, 0.568, 0.778), dtype=np.float),
//    'Residual': np.array((0.633, -0.713, 0.302), dtype=np.float),
//    'Background': np.array((255, 255, 255), dtype=np.uint8)
//}
//
//
//def color_to_HDAB(im_h, im_dab, stain):
//"""
//    im_h and im_dab are 2D arrays of same shape with values between 0(not present) and 1(max intensity)
//    """
//if not im_h.ndim == 2:
//print('Error: heamatoxylin image dimensions not 2')
//if not im_dab.ndim == 2:
//print('Error: DAB image dimensions not 2')
//if not im_h.shape == im_dab.shape:
//print('Error: shape not equal')
//return
//# create the image
//im_out = np.empty(shape=(3, *im_h.shape), dtype=np.uint8)
//for ch in range(3):
//im_out[ch] = stain['Background'][ch]
//im_out[ch] = _safe_subtr(im_out[ch],
//        np.array(im_h * stain['Hematoxylin'][ch] * stain['Background'][ch], dtype=np.uint8))
//im_out[ch] = _safe_subtr(im_out[ch],
//        np.array(im_dab * stain['DAB'][ch] * stain['Background'][ch], dtype=np.uint8))
//return im_out
//
//
//def _safe_subtr(a, b):
//msk = b > a
//a -= b
//a[msk] = 0
//return a




////// get intensity classifier threshold from qupath
//import qupath.lib.scripting.QP
//import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
//def hyr = QP.getCurrentHierarchy()
//def detobjects = hyr.getDetectionObjects()
//def measurementValues = detobjects.stream().mapToDouble(p -> p.getMeasurementList().getMeasurementValue('DAB: Mean'))
//        .filter(d -> Double.isFinite(d)).toArray()
//def stats = new DescriptiveStatistics(measurementValues)
//print(stats.getMean() + stats.getStandardDeviation())
//def intenMean = stats.getMean()
//def intenSdv = stats.getStandardDeviation()
//def intennum = 0.5        //// can be from 0 to 2
//def val = intenMean + intenSdv * intennum






//class CreateThreeChannelOp implements ImageOp {
//    def public stainvector = [0.651, 0.701, 0.29]
//    @Override
//    public Mat apply(Mat input) {
//        def channels = OpenCVTools.splitChannels(input)
//        if (channels.size() != 1){
//            print("Error: Need one channel")
//            return null
//        }
//        def red = org.bytedeco.opencv.global.opencv_core.multiply(channels[0], -stainvector[0]).asMat()
//        def green = org.bytedeco.opencv.global.opencv_core.multiply(channels[0], -stainvector[1]).asMat()
//        def blue = org.bytedeco.opencv.global.opencv_core.multiply(channels[0], -stainvector[2]).asMat()
//        def threechannels = [red, green, blue]
//        def combine = OpenCVTools.mergeChannels(threechannels, null)
//        return combine
//    }
//}



/////change chain name
//import qupath.lib.scripting.QP
//QP.setChannelNames('Hematoxylin (Disco Hematox)', 'CD68 (Disco Purple)', 'CD3 (Disco Teal)', 'PDL1 (Disco Yellow)')
//QP.setChannelColors(3817378, 16722632, 4244896, 16750861)


/////transform_objects.groovy 转移object到新image after Whole Slide Image (WSI) Registration
//import ch.epfl.biop.qupath.transform.TransformHelper
//
//// Get the current image data
//def imageData = getCurrentImageData()
//
//// Transfer all matching annotations and detections, keeps hierarchy,
//// transfer measurements (first true parameter)
//// allow to use inverse transforms (second true parameter)
//TransformHelper.transferMatchingAnnotationsToImage(imageData, true, true)
//
//// Computes all intensities measurements in the new image
//def server = getCurrentServer()
//TransformHelper.addIntensityMeasurements(getAnnotationObjects(), server, 1, true)
//
////If the image is RGB, this line can be added to import the correct measurements (DAB, etc.) :
////
////server = new qupath.lib.images.servers.TransformedServerBuilder(getCurrentServer())
////      .deconvolveStains(getCurrentImageData().getColorDeconvolutionStains(), 1, 2)
////      .build()
////cf https://forum.image.sc/t/transferring-segmentation-predictions-from-custom-masks-to-qupath/43408/15



//import java.awt.geom.AffineTransform
//def transform = new AffineTransform(
//        matrix[0], matrix[3], matrix[1],
//        matrix[4], matrix[2], matrix[5]
//)




//转换32bit to 8bit
//import qupath.lib.color.ColorModelFactory
//import qupath.lib.images.servers.ImageServer
//import qupath.lib.images.servers.ImageServerBuilder
//import qupath.lib.images.servers.ImageServerMetadata
//import qupath.lib.images.servers.PixelType
//import qupath.lib.images.servers.TransformingImageServer
//import qupath.lib.images.writers.ome.OMEPyramidWriter
//import qupath.lib.regions.RegionRequest
//
//import java.awt.image.BufferedImage
//import java.awt.image.DataBuffer
//import java.awt.image.WritableRaster
//
//import static qupath.lib.gui.scripting.QPEx.*
//
//def imageData = getCurrentImageData()
//
//// Output server path
//def path = buildFilePath(PROJECT_BASE_DIR, 'converted-8bit-other.ome.tif')
//
//// Create a scaling & bit-depth-clipping server
//def server = new TypeConvertServer(imageData.getServer(), 100f, 0f)
//
//// Write the pyramid
//new OMEPyramidWriter.Builder(server)
//        .parallelize()
//        .downsamples(server.getPreferredDownsamples())
//        .bigTiff()
//        .channelsInterleaved()
//        .build()
//        .writePyramid(path)
//print 'Done!'
//
//class TypeConvertServer extends TransformingImageServer<BufferedImage> {
//
//    private float scale = 1f
//    private float offset = 0
//    private ImageServerMetadata originalMetadata
//    def cm = ColorModelFactory.getDummyColorModel(8)
//
//    protected TypeConvertServer(ImageServer<BufferedImage> server, float scale, float offset) {
//        super(server)
//        this.scale = scale
//        this.offset = offset
//        this.originalMetadata = new ImageServerMetadata.Builder(server.getMetadata())
//                .pixelType(PixelType.UINT8)
//                .build()
//    }
//
//    public ImageServerMetadata getOriginalMetadata() {
//        return originalMetadata
//    }
//
//    @Override
//    protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
//        throw new UnsupportedOperationException()
//    }
//
//    @Override
//    protected String createID() {
//        return TypeConvertServer.class.getName() + ": " + getWrappedServer().getPath() + " scale=" + scale + ", offset=" + offset
//    }
//
//    @Override
//    String getServerType() {
//        return "Type converting image server"
//    }
//
//    public BufferedImage readBufferedImage(RegionRequest request) throws IOException {
//        def img = getWrappedServer().readBufferedImage(request);
//        def raster = img.getRaster()
//        int nBands = raster.getNumBands()
//        int w = img.getWidth()
//        int h = img.getHeight()
//        def raster2 = WritableRaster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, nBands, null)
//        float[] pixels = null
//        for (int b = 0; b < nBands; b++) {
//            pixels = raster.getSamples(0, 0, w, h, b, (float[])pixels)
//            for (int i = 0; i < w*h; i++) {
//                pixels[i] = (float)GeneralTools.clipValue(pixels[i] * scale + offset, 0, 255)
//            }
//            raster2.setSamples(0, 0, w, h, b, pixels)
//        }
//        return new BufferedImage(cm, raster2, false, null)
//    }
//
//}



//////exporting a labeled image for the bounding box of each annotation (mask)
//import qupath.lib.common.ColorTools
//import qupath.lib.common.GeneralTools
//import qupath.lib.images.servers.LabeledImageServer
//import qupath.lib.scripting.QP
//
//def imageData = QP.getCurrentImageData()
//def server = imageData.getServer()
//def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
//
//// Define output path (relative to project)
//def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
//def pathOutput = QP.buildFilePath(PROJECT_BASE_DIR, 'export', name)
//mkdirs(pathOutput)
//
//// Define output resolution
//double requestedPixelSize = 0.25
//// Convert to downsample
//double downsample = requestedPixelSize / imageData.getServer().getPixelCalibration().getAveragedPixelSize()
//
//// Create an ImageServer where the pixels are derived from annotations
//def labelServer = new LabeledImageServer.Builder(imageData)
//        .backgroundLabel(0, ColorTools.BLACK) // Specify background label (usually 0 or 255)
////    .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
////    .addLabel('Testing', 1)      // Choose output labels (the order matters!)
//        .addLabel('Tumour cells', 1)
////    .addLabel('Other', 3)
//        .lineThickness(0)          // Optionally export annotation boundaries with another label
//        .setBoundaryLabel('Boundary*', 1) // Define annotation boundary label
//        .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
//        .build()
//
//// Export each region
//int i = 0
//for (annotation in getAnnotationObjects()) {
//    def region = RegionRequest.createInstance(
//            labelServer.getPath(), downsample, annotation.getROI())
//    i++
//    def outputPath = buildFilePath(pathOutput, 'Region ' + i + '.png')
//    writeImageRegion(labelServer, region, outputPath)
//}
//println 'Done!'

////一样
////////exporting a Full labeled image (mask)
//import qupath.lib.common.ColorTools
//import qupath.lib.common.GeneralTools
//import qupath.lib.images.servers.LabeledImageServer
//import qupath.lib.scripting.QP
//
//def project = QP.getProject()
//def imageData = QP.getCurrentImageData()
//
//// Define output path (relative to project)
//def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
//def outputDir = QP.buildFilePath(PROJECT_BASE_DIR, 'export')
//def path = QP.buildFilePath(outputDir, name + "-labels.png")
//mkdirs(outputDir)
//
//// Define how much to downsample during export (may be required for large images)
//double downsample = 8
//
//// Create an ImageServer where the pixels are derived from annotations
//def labelServer = new LabeledImageServer.Builder(imageData)
//        .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
////  .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
////        .addLabel('Tumour cells', 1)      // Choose output labels (the order matters!)
////  .addLabel('Stroma', 2)
////  .addLabel('Other', 3)
//        .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
//        .build()
//
//// Write the image
//writeImage(labelServer, path)
//
//println 'Done!'

/////  这个最好   ///直接对detection处理变成mask
//import qupath.lib.common.ColorTools
//import qupath.lib.common.GeneralTools
//import qupath.lib.scripting.QP
//import qupath.lib.images.servers.LabeledImageServer
//
//def project = QP.getProject()
//def imageData = QP.getCurrentImageData()
//def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
//def pathOutput = QP.buildFilePath(project.getPath().parent as String, 'export', name + "-labels.png")
//
//def labelServer = new LabeledImageServer.Builder(imageData)
//        .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
//        .downsample(1)
//        .useInstanceLabels()
//        .useDetections()
//        .build()
//
//print(pathOutput)
//QP.writeImage(labelServer, pathOutput)
//println 'Done!'



////创建全图annotation
//QP.createSelectAllObject(true)


//clearDetections()


/////量长度
//print "Selected Line length is " + Math.round(getSelectedObject().getROI().getLength()) + " pixels"



////////////////找图片地址////////////////
//
//import qupath.lib.scripting.QP
//def server = QP.getCurrentServer()
//def myuris = server.getURIs()
//for (uri in myuris) {
//    println(uri) }
//
////  https://slidescore.nki.nl/i/66672/BlrTu1MB3LMW0m128b/SlideScoreMetadata.json



// FragmentSize  HoleSizeMicrons
//runPlugin('qupath.lib.plugins.objects.RefineAnnotationsPlugin', '{"minFragmentSizeMicrons":50000.0,"maxHoleSizeMicrons":4500.0}')
//runPlugin('qupath.lib.plugins.objects.SplitAnnotationsPlugin', '{}')





