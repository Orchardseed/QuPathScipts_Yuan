

import qupath.lib.scripting.QP

def imageData = QP.getCurrentImageData()
def annotations = QP.getAnnotationObjects()
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
print(name)
def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'export', name)
def path = pathOutput +".geojson"
// 'FEATURE_COLLECTION' is standard GeoJSON format for multiple objects
exportObjectsToGeoJson(annotations, path, "FEATURE_COLLECTION")
print 'Done'


