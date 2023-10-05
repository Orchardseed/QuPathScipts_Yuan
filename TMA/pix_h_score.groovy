import qupath.lib.images.servers.PixelCalibration
import qupath.lib.objects.classes.PathClass
import qupath.lib.scripting.QP
import qupath.opencv.ml.pixel.PixelClassifiers
import qupath.opencv.ops.ImageOps
import java.nio.file.Path

//SET THESE
double[] DABthresholds=[0.15,0.5,1] //1+, 2+, 3+ DAB
double Hthreshold=0.03 //Nuclei in Hematoxylin
double scale = 1 //bigger = downsample for speed
String annotClass="Tumor" //what class are your annotations?

//make pixel_classifiers folder
String pixelClassifierFolder=QP.buildFilePath(QP.PROJECT_BASE_DIR,'classifiers','pixel_classifiers') //project subfolder with pixel classifier
QP.mkdirs(pixelClassifierFolder)

//name classes and classifiers
String[] classes = ["1+", "2+", "3+"]
String[] classifiers = ["1.json", "2.json", "3.json"]

//get pixel calibration and current stain vectors
def imageData = QP.getCurrentImageData()
PixelCalibration cal=imageData.getServer().getPixelCalibration()
def stains = QP.getCurrentImageData().getColorDeconvolutionStains()

//create pixel classifiers for DAB thresholds
for (int i=0; i<3; i++){
    //define preprocessing and thresholds
    def ops2 = [
            ImageOps.Channels.deconvolve(stains),
            ImageOps.Channels.extract(1), //H=0, DAB=1
            //  ImageOps.Filters.gaussianBlur(1), //1x gaussian smoothing
            ImageOps.Threshold.threshold(DABthresholds[i]), //threshold for DAB channel
    ]
    def op = ImageOps.buildImageDataOp().appendOps(*ops2) //turn into single ImageOp
    Map<Integer, PathClass> classmap=Map.of(1,QP.getPathClass(classes[i])) //define intensity 1 = positive
    pixclass=PixelClassifiers.createClassifier(op,cal.createScaledInstance(scale,scale),classmap) //create the classifier

    //write new classifier to file
    Path writepath=Path.of(pixelClassifierFolder,classifiers[i])
    PixelClassifiers.writeClassifier(pixclass,writepath)

    //measure the area of each class
    QP.selectObjectsByClassification(annotClass)
    QP.addPixelClassifierMeasurements((i+1).toString(), (i+1).toString())
}

//for the H score denominator, get all the pixels that are either DAB-positive or Hematoxylin-positive
//important not to double count double-positive pixels
def opsdouble = [
        ImageOps.Channels.deconvolve(stains),
        ImageOps.Channels.extract(0,1), //choose the channels for thresholding (0-based)
        //  ImageOps.Filters.gaussianBlur(1), //1x gaussian smoothing
        ImageOps.Threshold.threshold(Hthreshold,DABthresholds[0]), //threshold for each channel, matching the order of the extraction
        ImageOps.Channels.maximum()  //maximum creates the union of the two thresholds
]
def opdouble = ImageOps.buildImageDataOp().appendOps(*opsdouble)

Map<Integer, PathClass> classmap=Map.of(1,QP.getPathClass('HDAB')) //define intensity 1 = newPathClass
pixclass=PixelClassifiers.createClassifier(opdouble,cal.createScaledInstance(scale,scale),classmap) //create the classifier

//write new classifier to file
Path writepath=Path.of(pixelClassifierFolder,'HDAB.json')
PixelClassifiers.writeClassifier(pixclass,writepath)

QP.selectObjectsByClassification(annotClass);
QP.addPixelClassifierMeasurements("HDAB", "HDAB") //measure H-DAB class

def annots=QP.getAnnotationObjects().findAll{it.getPathClass()==QP.getPathClass(annotClass)} //get objects to measure

//measure area of each class
annots.each{annot->
    double area1=annot.measurements["1: 1+ area µm^2"]
    double area2=annot.measurements["2: 2+ area µm^2"]
    double area3=annot.measurements["3: 3+ area µm^2"]
    double areaDenom=annot.measurements["HDAB: HDAB area µm^2"]

    annot.measurements['Pixelwise H-score']=(area1+area2+area3)/(areaDenom)*100 //calculate H score
//NOTE: 1+ area INCLUDES 2+ and 3+. Therefore area1 + area2 + area3 = 1*DAB_low + 2*DAB_med + 3*DAB_high.
//no need for subtraction and re-addition
}
