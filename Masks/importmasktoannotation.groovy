
import qupath.lib.objects.PathObjects
import qupath.lib.regions.ImagePlane
import ij.IJ
import ij.process.ColorProcessor
import qupath.imagej.processing.RoiLabeling
import qupath.imagej.tools.IJTools
import qupath.lib.scripting.QP
import qupath.lib.common.GeneralTools



def directoryPath = 'D:\\QuPath\\QuPath projects\\BIF\\yg.bif\\Mask_IM_EX\\masks' // TO CHANGE
File folder = new File(directoryPath);
File[] listOfFiles = folder.listFiles();


double downsample = 8.144      // TO CHANGE (if needed)
ImagePlane plane = ImagePlane.getDefaultPlane()

currentImport = listOfFiles.find{ GeneralTools.getNameWithoutExtension(it.getPath()).contains(GeneralTools.getNameWithoutExtension(getProjectEntry().getImageName())) &&  it.toString().contains(".png")}
path = currentImport.getPath()
def imp = IJ.openImage(path)


int n = imp.getStatistics().max as int
if (n == 0) {
    print 'No objects found!'
    return
}
def ip = imp.getProcessor()
if (ip instanceof ColorProcessor) {
    throw new IllegalArgumentException("RGB images are not supported!")
}
def roisIJ = RoiLabeling.labelsToConnectedROIs(ip, n)

def rois = roisIJ.collect {
    if (it == null)
        return
    return IJTools.convertToROI(it, 0, 0, downsample, plane);
}
rois = rois.findAll{null != it}

// Convert QuPath ROIs to objects
def pathObjects = rois.collect {
    return PathObjects.createAnnotationObject(it)
}
QP.addObjects(pathObjects)
QP.resolveHierarchy()
print "Import completed"


