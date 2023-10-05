import qupath.imagej.tools.IJTools
import qupath.lib.images.servers.ImageServer
import qupath.lib.images.servers.TransformedServerBuilder
import qupath.lib.objects.TMACoreObject
import qupath.lib.regions.RegionRequest
import qupath.lib.scripting.QP
import java.awt.image.BufferedImage

import static java.lang.Math.abs


def downsample = 16
def tolerance = 0.5
ImageServer<BufferedImage> myserver = QP.getCurrentServer() as ImageServer<BufferedImage>
def tsb = new TransformedServerBuilder(myserver).averageChannelProject().build()
def tmacorelist = QP.getTMACoreList().findAll({!it.isMissing()})

tmacorelist.eachWithIndex{ TMACoreObject entry, int i ->
    def roi = entry.getROI()
    def dx = 100
    def dy = 100
    def attempts = 0
    while(abs(dx)>tolerance || abs(dy)>tolerance){
        attempts+=1
        def region = RegionRequest.createInstance(myserver.getPath(), downsample, roi)
        def pthimgplus = IJTools.convertToImagePlus(tsb, region)
        def imgplus = pthimgplus.getImage()
        def processor = imgplus.getProcessor()
        processor.invert()
        def imwidth = processor.getWidth()
        def imheigt = processor.getHeight()
        def stats = processor.getStatistics()
        if(stats.mean>200){
            entry.setMissing(true)
            break
        }
        def xc = stats.xCenterOfMass
        def yc = stats.yCenterOfMass
        dx = imwidth/2 - xc
        dy = imheigt/2 - yc
        roi = roi.translate((-dx*downsample),(-dy*downsample))
        imgplus.close()
        if (attempts>20){
            break
        }
    }
    entry.setROI(roi)
    print("Core "+i+": "+entry.toString()+" - "+attempts+" iterations")
}
