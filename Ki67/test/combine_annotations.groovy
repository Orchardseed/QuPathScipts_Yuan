import qupath.lib.scripting.QP

def all_annotations = QP.getAnnotations()
unique_classes = []
all_annotations.forEach({
    if (it.getPathClass is notin unique_classes){unique_classes.add(it)}
})
print(unique_classes)
unique_classes.forEach({
    def current_class = it
    def combine = all_annotations.findall({it.getPathClass == current_class})
    QP.mergeAnnotations(combine)
})


QP.exportObjectsToGeoJson(annotations, path, "FEATURE_COLLECTION")
