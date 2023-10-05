import qupath.lib.images.servers.LabeledImageServer
import qupath.lib.scripting.QP
import qupath.lib.common.ColorTools
import qupath.lib.common.GeneralTools

def imageData = QP.getCurrentImageData()

// Define output path (relative to project)
def pathOutput = QP.buildFilePath(QP.PROJECT_BASE_DIR, 'masks')
mkdirs(pathOutput)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def path = QP.buildFilePath(pathOutput, name + "-labels.png")

// Define how much to downsample during export (may be required for large images)
double downsample = 5

// Create an ImageServer where the pixels are derived from annotations
def labelServer = new LabeledImageServer.Builder(imageData)
        .backgroundLabel(0, ColorTools.WHITE) // Specify background label (usually 0 or 255)
        .downsample(downsample)    // Choose server resolution; this should match the resolution at which tiles are exported
        .addLabel('tumor', 1)      // Choose output labels (the order matters!)
        .addLabel('stroma', 2)
        .addLabel('other classes', 3)
        .multichannelOutput(false) // If true, each label refers to the channel of a multichannel binary image (required for multiclass probability)
        .build()

// Write the image
QP.writeImage(labelServer, path)

print('Done')