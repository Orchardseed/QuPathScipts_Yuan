import qupath.lib.scripting.QP
import qupath.lib.color.ColorDeconvolutionStains
import qupath.lib.images.ImageData
import groovy.time.TimeCategory

// Run detection for the selected objects
def start = new Date()

def imageData = QP.getCurrentImageData()

////1
//// Set image type, if you want to do it manually, please comment out the following 2 lines
//imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
//def myStain = ColorDeconvolutionStains.parseColorDeconvolutionStainsArg('{"Name" : "H-DAB modified", "Stain 1" : "Hematoxylin", "Values 1" : "0.85 0.525 0.145", "Stain 2" : "DAB", "Values 2" : "0.32 0.5 0.8", "Background" : "255 255 255"}')
//imageData.setColorDeconvolutionStains(myStain)
//QP.createAnnotationsFromPixelClassifier("Tissue_1", 200000.0, 100000.0, "SELECT_NEW")
//QP.createAnnotationsFromPixelClassifier("Artefacts_2", 200000.0, 2500.0, "SPLIT")
//print('Done')
//QP.clearSelectedObjects(true)
//print('Done')

////2
imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
QP.createAnnotationsFromPixelClassifier("Tissue_1", 200000.0, 100000.0, "SPLIT")
print('Done')

QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus( stop, start )
println(td)


