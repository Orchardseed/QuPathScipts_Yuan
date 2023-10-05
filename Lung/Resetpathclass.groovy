import qupath.lib.scripting.QP

def imageData = QP.getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def detCells = hierarchy.getDetectionObjects()
def unclassified = detCells.findAll {it.getPathClass()==null}
def Negative = QP.getPathClass('Negative')
unclassified.forEach({
    it.setPathClass(Negative)
    it.setColor(128, 128, 128)
})




