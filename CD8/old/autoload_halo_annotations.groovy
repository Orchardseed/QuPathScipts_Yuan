import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import qupath.lib.images.ImageData
import qupath.lib.objects.PathObjects
import qupath.lib.objects.classes.PathClassFactory
import qupath.lib.regions.ImagePlane
import qupath.lib.roi.ROIs
import qupath.lib.roi.RoiTools
import qupath.lib.roi.interfaces.ROI

import java.lang.reflect.Type
import qupath.lib.scripting.QP
import groovy.io.FileType

def data_dir = 'D:\\QuPath\\QuPath projects\\BIF\\yg.bif\\IRBm21_183_CD_8\\error_anno\\'
// list all .annotation files
List<File> annotationfiles = []
def dir = new File(data_dir)
dir.eachFileRecurse (FileType.FILES) { file ->
    if (file.name.endsWith('.annotations')){
        annotationfiles.add(file)
    }
}
// create Roi is a helper function to create rois in qupath
def static createRoi(x,y, imageplane){
    double[] arrx = x.stream().mapToDouble(Double::doubleValue).toArray()
    double[] arry = y.stream().mapToDouble(Double::doubleValue).toArray()
    return ROIs.createPolygonROI(arrx,arry,imageplane)
}
// read all offsetdata
class offset_values {
    @SerializedName("COMPRESSED_STITCHING_ORIG_SLIDE_SCANNED_AREA_IN_PIXELS__LEFT = ")
    private Double left
    @SerializedName("COMPRESSED_STITCHING_ORIG_SLIDE_SCANNED_AREA_IN_PIXELS__TOP = ")
    private Double top

    final Double gettop(){
        return this.top
    }
    final Double getleft(){
        return this.left
    }
}
class offset {
    @SerializedName("Slidedat")
    private String Slidedat
    @SerializedName("Filename")
    private String Filename
    @SerializedName("values")
    private offset_values values
    final gettop(){
        return this.values.gettop()
    }
    final getleft(){
        return this.values.getleft()
    }
    final getname(){
        return this.Filename
    }
}
def offsetfile = data_dir + "results.json"
def offsetfiletxt = new File(offsetfile).text
Type collectionType = new TypeToken<Collection<offset>>(){}.getType()
Collection<offset> offsetdata = new Gson().fromJson(offsetfiletxt, collectionType)

// Load the annotations in the project
def project = QP.getProject()
def imagelist = project.getImageList()
def name = ''
Double top = 0.0
Double left = 0.0
File annotationfile = new File('')
def go = 0
imagelist.forEach({
    go = 0
    name = it.imageName.substring(0, it.imageName.length()-2).replace(' ', '_').replace('-','_') // it.imageName.length()'-2', make sure the characters at the end of files
    offsetdata.forEach({
        if (it.getname() == name){
            print("found "+name+" in offsets")
            top = it.gettop()
            left = it.getleft()
            go += 1
        }
    })
    annotationfiles.forEach({
        if (it.name.substring(0, it.name.length()-12) == name){
            annotationfile = it
            print("found "+name+" in annotations")
            go += 1
        }
    })
    if (go == 2){
        print("Will add annotation to "+name)
        // do stuff here with top, left, and annotationfile
        def imageData = it.readImageData()
        def hierarchy = imageData.getHierarchy()
        hierarchy.clearAll()
        imageData.setImageType(ImageData.ImageType.BRIGHTFIELD_H_DAB)
        def xmlData = new XmlSlurper().parse(annotationfile)
        def imageplane = ImagePlane.getDefaultPlane()
        def pathclass = PathClassFactory.getPathClass('Halo ROI')
        List<ROI> p_rois = []
        List<ROI> n_rois = []
        xmlData.childNodes().eachWithIndex{ def annotation, int a_idx ->
            p_rois = []
            n_rois = []
            def a_attrs = annotation.attributes()
            a_attrs.keySet().forEach{ def a_key ->
                if (a_key == 'Name'){
                    pathclass = PathClassFactory.getPathClass(a_attrs[a_key])
                }
            }
            annotation.childNodes().eachWithIndex { def regions, int a2_idx ->
                regions.childNodes().eachWithIndex { def region, int r_idx ->
                    def r_attrs = region.attributes()
                    r_attrs.keySet().forEach{ def r_key ->
                    }
                    def negative = r_attrs['NegativeROA']=='1'
                    region.childNodes().eachWithIndex { def vertices_comments, int vc_idx ->
                        if (vertices_comments.name == 'Vertices'){
                            List<Double> x = []
                            List<Double> y = []
                            vertices_comments.childNodes().eachWithIndex { def vertices, int v_idx ->
                                x.add(Double.parseDouble(vertices.attributes()['X'] as String)+left)
                                y.add(Double.parseDouble(vertices.attributes()['Y'] as String)+top)
                            }
                            if (negative){
                                n_rois.add(createRoi(x,y,imageplane))
                            }else{
                                p_rois.add(createRoi(x,y,imageplane))
                            }
                        }
                    }
                }
            }
            List<ROI> rois = []
            if (n_rois.size()==0){
                rois = p_rois
            }else { // subtract the negative rois from the positive rois
                n_rois.each { ROI nroi ->
                    p_rois.each { ROI proi ->
                        rois.add(RoiTools.combineROIs(proi, nroi, RoiTools.CombineOp.SUBTRACT))
                    }
                }
            }
            // add the rois
            rois.each { ROI r ->
                def new_annotation = PathObjects.createAnnotationObject(r, pathclass)
                hierarchy.addPathObject(new_annotation)
            }
        }
        it.saveImageData(imageData)
    }else {
        print("Will NOT add annotation to "+name)
    }
})
