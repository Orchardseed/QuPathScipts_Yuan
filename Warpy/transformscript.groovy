import qupath.lib.scripting.QP
import qupath.lib.awt.common.AffineTransforms

QP.transformSelectedObjects(
        AffineTransforms.fromRows(
                1.04033, 0.00015,
                -69297.86290, 0.01288,
                0.97602, 30874.18797).createInverse())

