// requires imageData as a global


import org.apache.commons.io.filefilter.TrueFileFilter
import qupath.lib.scripting.QP


def cores = QP.getTMACoreList().findAll({!it.isMissing()})
print cores
int i = 0

for (core in cores){
    print core
    i++
}

print i
//
//def cores = QP.getTMACoreList()
//print cores.getClass()


