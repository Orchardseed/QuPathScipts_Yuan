import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.color.ColorDeconvolutionStains
import qupath.lib.gui.scripting.QPEx
import qupath.lib.images.ImageData
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
import static qupath.lib.scripting.QP.setColorDeconvolutionStains
import qupath.lib.classifiers.PathClassifierTools
import org.slf4j.LoggerFactory

def project = QP.getProject()

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

    // Set image type, if you want to do it manually, please comment out the following 2 lines
    imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
    //, ColorDeconvolutionStains('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.886 0.449 0.114", "Stain 2" : "DAB", "Values 2" : "0.356 0.519 0.777", "Background" : " 255 255 255"}'))
//    QP.setImageType('BRIGHTFIELD_H_DAB')
    setColorDeconvolutionStains('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.886 0.449 0.114", "Stain 2" : "DAB", "Values 2" : "0.356 0.519 0.777", "Background" : " 255 255 255"}');
    println 'set Image Type Done!'

    entry.saveImageData(imageData)
}

