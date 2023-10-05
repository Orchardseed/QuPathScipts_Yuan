import org.slf4j.LoggerFactory
import qupath.lib.color.ColorDeconvolutionStains
import qupath.lib.images.ImageData
import qupath.lib.io.PathIO
import qupath.lib.scripting.QP
import java.nio.file.FileSystems
import java.nio.file.Files

def logger = LoggerFactory.getLogger("import_all_annotations")
def proj = QP.getProject()
for (projectImageEntry in proj.getImageList()) {
    def imagename = projectImageEntry.getImageName()
    proj.getPath().parent.toString()
    def inpth = FileSystems.getDefault().getPath(proj.getPath().parent as String, imagename.substring(0,10)+".geojson")
    logger.info(inpth.toString())
    if (Files.exists(inpth)){
        def objs = PathIO.readObjects(new File(inpth as String))
        def imageData = projectImageEntry.readImageData()
        imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
        def myStain = ColorDeconvolutionStains.parseColorDeconvolutionStainsArg('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.81349098 0.57214624 0.10431251", "Stain 2" : "DAB", "Values 2" : "0.1675769  0.3998422  0.90113495", "Background" : "255 255 255"}')
        imageData.setColorDeconvolutionStains(myStain)
        imageData.getHierarchy().addObjects(objs)
        projectImageEntry.saveImageData(imageData)
    }else {
        logger.info("SKIPPING...")
    }
}