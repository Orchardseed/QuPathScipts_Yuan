import qupath.lib.projects.ProjectImageEntry
import qupath.lib.scripting.QP
import org.slf4j.LoggerFactory

def logger = LoggerFactory.getLogger("select_from_project")
// locate file with names
def project = QP.getProject()
def location = project.getPath()
def file = new File(location.parent.toString(), 'samples.txt')
// read names
def samples = file.text.readLines()
// transform names to match names in slidescore
List<String> samples_n = []
samples.forEach({
    samples_n.add(it.substring(0,2)+"-"+it.substring(2,5)+"-"+it.substring(5,8)+"-01A")
})
// print result
logger.info("Found "+samples_n.size()+" samples: " + samples_n)
// locate the images and add them to an Arraylist
List<ProjectImageEntry> keep_images = []
List<ProjectImageEntry> remove_images = []
project.getImageList().forEach({
    if(it.getImageName().substring(0,14) in samples_n){
        keep_images.add(it)
    }else{
        remove_images.add(it)
    }
})
logger.info("Removing images: "+remove_images.size())
project.removeAllImages(remove_images, true)
project.syncChanges()
logger.info("Remaining images: "+project.getImageList().size())
