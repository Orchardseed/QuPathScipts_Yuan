import com.google.gson.Gson
import qupath.lib.io.GsonTools
import qupath.lib.scripting.QP
boolean prettyPrint = true


//// Set image type, if you want to do it manually, please comment out the following 2 lines
def imageData = QP.getCurrentImageData()
imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
QP.setColorDeconvolutionStains('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.886 0.449 0.114", "Stain 2" : "DAB", "Values 2" : "0.356 0.519 0.777", "Background" : " 255 255 255"}')

def gson = GsonTools.getInstance(prettyPrint)
def pos_cell_det_props = [
        "detectionImageBrightfield": "Optical density sum",
        "requestedPixelSizeMicrons": 0.485,
        "backgroundRadiusMicrons": 10.0,
        "medianRadiusMicrons": 1.0,
        "sigmaMicrons": 1.5,
        "minAreaMicrons": 10.0,
        "maxAreaMicrons": 50.0,
        "threshold": 0.05,
        "maxBackground": 1.0,
        "watershedPostProcess": true,
        "excludeDAB": false,
        "cellExpansionMicrons": 5.0,
        "includeNuclei": true,
        "smoothBoundaries": true,
        "makeMeasurements": true,
        "thresholdCompartment": "Cell: DAB OD mean",
        "thresholdPositive1": 0.1,
        "thresholdPositive2": 0.4,
        "thresholdPositive3": 0.6,
        "singleThreshold": true
]
QP.runPlugin('qupath.imagej.detect.cells.PositiveCellDetection', gson.toJson(pos_cell_det_props))