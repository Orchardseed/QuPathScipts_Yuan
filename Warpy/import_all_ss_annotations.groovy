import qupath.lib.images.servers.slidescore.SlideScoreImageServer
import qupath.lib.objects.classes.PathClass
import qupath.lib.scripting.QP
import qupath.lib.gui.QuPathGUI
import qupath.lib.images.servers.slidescore.SlideScoreImportAnswersCommand

qupathGUI = QuPathGUI.getInstance()
def slideScoreImportAnswersCommand = new SlideScoreImportAnswersCommand(qupathGUI)
def imageData = QP.getCurrentImageData()
SlideScoreImageServer server = (SlideScoreImageServer) imageData.getServer()
def questions = server.getAnnotationQuestions()
def annotations = QP.getAllObjects()
questions.each {
    def pathclass = PathClass.fromString(it, 0)
    slideScoreImportAnswersCommand.setEmail("k.d.bie@nki.nl")
    slideScoreImportAnswersCommand.setQuestion(it)
    slideScoreImportAnswersCommand.setNames=false
//    slideScoreImportAnswersCommand.disableNames()
    slideScoreImportAnswersCommand.run(imageData)
    def new_annotations = QP.getAllObjects().findAll({!annotations.contains(it)})
    new_annotations.each({
        it.setPathClass(pathclass)
        it.setColor(255, 0, 0)
    })
    annotations = QP.getAllObjects()
}
print('end')