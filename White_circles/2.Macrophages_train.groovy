import qupath.ext.biop.cellpose.Cellpose2D
import qupath.lib.scripting.QP
import groovy.time.TimeCategory
import org.slf4j.LoggerFactory


// Run detection for the selected objects
def start = new Date()
def imageData = QP.getCurrentImageData()
def server = imageData.getServer()
def originalPixelSize = server.getPixelCalibration().getAveragedPixelSizeMicrons()
def logger = LoggerFactory.getLogger("base script")
logger.info("The pixelsize is "+originalPixelSize)
//print(originalPixelSize)


// Cell segmentation (Cellpose) Specify the model name (cyto, nuc, cyto2 or a path to your custom model)
def pathModel = 'D:\\QuPath\\Cellpose_models\\cytotorch_0'
def cellpose = Cellpose2D.builder(pathModel)
        .channels('DAPI')
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(originalPixelSize)              // Resolution for detection
        .epochs(500)              // Optional: will default to 500
        .learningRate(0.2)        // Optional: Will default to 0.2
        .batchSize(8)             // Optional: Will default to 8
        .modelDirectory(new File("D:\\QuPath\\QuPath projects\\BIF\\yg.bif\\Script\\White_circles\\models")) // Optional place to store resulting model. Will default to QuPath project root, and make a 'models' folder
        .useGPU()                 // Optional: Use the GPU if configured, defaults to CPU only
        .build()


def resultModel = cellpose.train()

// Pick up results to see how the training was performed
println "Model Saved under "
println resultModel.getAbsolutePath().toString()

// You can get a ResultsTable of the training.
def results = cellpose.getTrainingResults()
results.show("Training Results")

// You can get a results table with the QC results to visualize
def qcResults = cellpose.getQCResults()
qcResults.show("QC Results")

// Finally you have access to a very simple graph
cellpose.showTrainingGraph()
//// Save
QP.getProjectEntry().saveImageData(imageData)
stop = new Date()
td = TimeCategory.minus(stop, start)
println(td)

