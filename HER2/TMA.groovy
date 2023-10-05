// requires imageData as a global
import qupath.lib.gui.QuPathGUI
import qupath.lib.scripting.QP
import qupath.lib.images.servers.slidescore.SlideScoreImportTMAsCommand

// load TMA positions from slidescore
def qupathGUI = QuPathGUI.getInstance()
def slideScoreImportTMAsCommand = new SlideScoreImportTMAsCommand(qupathGUI)
imageData = QP.getCurrentImageData()

slideScoreImportTMAsCommand.run(imageData)

def hierarchy = QP.getCurrentHierarchy()
def grid = hierarchy.getTMAGrid()
def cols = grid.getGridWidth()
def rows = grid.getGridHeight()

// get mean xstep and mean ystep for rows and columns
def mean_xstep_row = 0
def mean_ystep_row = 0
def n_step = 0
for (int rownr = 0; rownr<rows; rownr++) {
    for (int colnr = 1; colnr < cols; colnr++) {
        def tmaCore0 = grid.getTMACore(rownr, colnr-1)
        def tmaCore1 = grid.getTMACore(rownr, colnr)
        if (tmaCore0.isTMACore() && tmaCore1.isTMACore() &&
                !tmaCore0.isMissing() && !tmaCore1.isMissing()) {
            def roi0 = tmaCore0.getROI()
            def roi1 = tmaCore1.getROI()
            mean_xstep_row += (roi1.getCentroidX() - roi0.getCentroidX())
            mean_ystep_row += (roi1.getCentroidY() - roi0.getCentroidY())
            n_step += 1
        }
    }
}
mean_xstep_row /= n_step
mean_ystep_row /= n_step

def mean_xstep_col = 0
def mean_ystep_col = 0
n_step = 0
for (int rownr = 1; rownr<rows; rownr++) {
    for (int colnr = 0; colnr < cols; colnr++) {
        def tmaCore0 = grid.getTMACore(rownr-1, colnr)
        def tmaCore1 = grid.getTMACore(rownr, colnr)
        if (tmaCore0.isTMACore() && tmaCore1.isTMACore() &&
                !tmaCore0.isMissing() && !tmaCore1.isMissing()) {
            def roi0 = tmaCore0.getROI()
            def roi1 = tmaCore1.getROI()
            mean_xstep_col += (roi1.getCentroidX() - roi0.getCentroidX())
            mean_ystep_col += (roi1.getCentroidY() - roi0.getCentroidY())
            n_step += 1
        }
    }
}
mean_xstep_col /= n_step
mean_ystep_col /= n_step

// go over all TMA's and find missing ones
for (int rownr = 0; rownr<rows; rownr++) {
    for (int colnr = 0; colnr < cols; colnr++) {
        def tmaCore = grid.getTMACore(rownr, colnr)
        if (tmaCore.isTMACore() && tmaCore.isMissing()) {
            // Try to find four non-missing TMA's in four directions and infer coordinates for missing one
            def x = 0.0
            def y = 0.0
            def n = 0
            def searchRow = rownr-1
            while (searchRow>-1){
                def tmaCoreS = grid.getTMACore(searchRow, colnr)
                if (tmaCoreS.isTMACore() && !tmaCoreS.isMissing()){
                    def roiS = tmaCoreS.getROI()
                    n_step = searchRow-rownr
                    x += roiS.getCentroidX() - n_step*mean_xstep_col
                    y += roiS.getCentroidY() - n_step*mean_ystep_col
                    n +=1
                    searchRow = -1
                }
                searchRow -= 1
            }
            searchRow = rownr+1
            while (searchRow<rows){
                def tmaCoreS = grid.getTMACore(searchRow, colnr)
                if (tmaCoreS.isTMACore() && !tmaCoreS.isMissing()){
                    def roiS = tmaCoreS.getROI()
                    n_step = searchRow-rownr
                    x += roiS.getCentroidX() - n_step*mean_xstep_col
                    y += roiS.getCentroidY() - n_step*mean_ystep_col
                    n +=1
                    searchRow = rows
                }
                searchRow += 1
            }
            def searchCol = colnr-1
            while (searchCol>-1){
                def tmaCoreS = grid.getTMACore(rownr, searchCol)
                if (tmaCoreS.isTMACore() && !tmaCoreS.isMissing()){
                    def roiS = tmaCoreS.getROI()
                    n_step = searchCol-colnr
                    x += roiS.getCentroidX() - n_step*mean_xstep_row
                    y += roiS.getCentroidY() - n_step*mean_ystep_row
                    n +=1
                    searchCol = -1
                }
                searchCol -= 1
            }
            searchCol = colnr+1
            while (searchCol<cols){
                def tmaCoreS = grid.getTMACore(rownr, searchCol)
                if (tmaCoreS.isTMACore() && !tmaCoreS.isMissing()){
                    def roiS = tmaCoreS.getROI()
                    n_step = searchCol-colnr
                    x += roiS.getCentroidX() - n_step*mean_xstep_row
                    y += roiS.getCentroidY() - n_step*mean_ystep_row
                    n +=1
                    searchCol = cols
                }
                searchCol += 1
            }
            x /= (double) n
            y /= (double) n
            def roi = tmaCore.getROI()
            tmaCore.setROI(roi.translate(x, y))
        }
    }
}
QP.fireHierarchyUpdate()