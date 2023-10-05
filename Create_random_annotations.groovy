import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.regions.ImagePlane
import qupath.lib.objects.PathCellObject

// Detect the areas where the tissue is located
//createAnnotationsFromPixelClassifier("Tissue detector", 0.0, 0.0, "SPLIT")

def annotations = getAnnotationObjects()

// Keep only the annotation which has the largest area
def max_area = annotations[0].getROI().getArea()
def max_indice = 0
for (i=0; i < annotations.size(); i++){
    if (annotations[i].getROI().getArea() >= max_area){
        max_area = annotations[i].getROI().getArea()
        max_indice = i
    }
} 
for (i in annotations){
    if (i.getROI().getArea()<max_area){
        removeObject(i,true)
    }
}

/***** Tile creation *****/ 
selectAnnotations()
runPlugin('qupath.lib.algorithms.TilerPlugin', '{"tileSizeMicrons": 1024.0,  "trimToROI": true,  "makeAnnotations": true,  "removeParentAnnotation": true}');

// Create variable for tiles
def tiles = getAnnotationObjects().findAll{ann -> ann.getName().startsWith("Tumour") }

/***** Tile filtering *****/ 
Random random = new Random()
tilesToProcess = []
for (i=0; i < 4; i++) {
     rand = Math.abs( new Random().nextInt() % tiles.size())
     tile = tiles[rand]
     tilesToProcess.add(tile)
}

tilesToRemove = tiles.findAll(tile -> !tilesToProcess.contains(tile))
removeObjects(tilesToRemove, true)

// Save your project (needed for exporting)
def imageData = getCurrentImageData()
getProjectEntry().saveImageData(imageData)

print "Done!"