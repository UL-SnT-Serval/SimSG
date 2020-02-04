package duc.aintea.tests.loadapproximation;

import duc.aintea.tests.sg.Entity;
import duc.aintea.tests.sg.Fuse;
import duc.aintea.tests.sg.Substation;
import duc.aintea.tests.utils.CycleDetection;
import duc.aintea.tests.utils.Matrix;

import java.util.*;

public class MatrixBuilder {


    private void setMatrix(Matrix matrix, int row, int column, double value) {
        if(row >= matrix.getNumRows()) {
            matrix.addLines(row - matrix.getNumRows() + 1);
        }

        if(column >= matrix.getNumCols()) {
            matrix.addColumns(column - matrix.getNumCols() + 1);
        }
        matrix.set(row, column, value);
    }

    public double[] build(Substation substation) {
        final var idxFuses = new HashMap<String, Integer>();
        var idxLast = new int[]{-1};

        final var cableEq = new Matrix();
        final var cabinetEq = new Matrix();
        final var circleEq = new Matrix();

        var waitingList = new ArrayDeque<Entity>();
        var entityVisited = new HashSet<Entity>();
        var fuseInCircles = new HashSet<Fuse>();

        waitingList.add(substation);

        while(!waitingList.isEmpty()) {
            var currEntity = waitingList.remove();
            var notYetVisited = entityVisited.add(currEntity);

            if(notYetVisited) {
                final List<Fuse> fuses = currEntity.getClosedFuses();

                if (!(currEntity instanceof Substation) && fuses.size() > 1) {
                    cabinetEq.addLine();
                }

                for (Fuse fuse : fuses) {
                        if (!idxFuses.containsKey(fuse.getName())) {
                            Fuse oppFuse = fuse.getOpposite();
                            cableEq.addLine();
                            int idxFuse = getOrCreateIdx(fuse, idxFuses, idxLast);
                            setMatrix(cableEq,cableEq.getNumRows() - 1, idxFuse, 1);

                            if (oppFuse.isClosed() && !oppFuse.getOwner().isDeadEnd()) {
                                int idxOpp = getOrCreateIdx(oppFuse, idxFuses, idxLast);
                                setMatrix(cableEq, cableEq.getNumRows() - 1, idxOpp, 1);
                                if (!entityVisited.contains(oppFuse.getOwner())) {
                                    waitingList.add(oppFuse.getOwner());
                                }
                            }
                        }

                        if (!(currEntity instanceof Substation) && fuses.size() > 1) {
                            int idxFuse = getOrCreateIdx(fuse, idxFuses, idxLast);
                            setMatrix(cabinetEq, cabinetEq.getNumRows() - 1, idxFuse, 1);
                        }

                        if(!fuseInCircles.contains(fuse)) {
                            Fuse[] circle = new CycleDetection().getEndCircle(fuse);
                            if(circle.length != 0) {
                                Collections.addAll(fuseInCircles, circle);

                                Fuse fuseEnd = circle[0];
                                int idxFuse = getOrCreateIdx(fuse, idxFuses, idxLast);
                                int idxFuseEnd = getOrCreateIdx(fuseEnd, idxFuses, idxLast);

                                circleEq.addLine();
                                setMatrix(circleEq,circleEq.getNumRows() - 1, idxFuse, 1);
                                setMatrix(circleEq,circleEq.getNumRows() - 1, idxFuseEnd, -1);
                            }
                        }
                }
            }
        }

        if(circleEq.getNumCols() < cableEq.getNumCols()) {
            circleEq.addColumns(cableEq.getNumCols() - circleEq.getNumCols());
        }

//        System.out.println(cableEq);
//        System.out.println();
//        System.out.println(cabinetEq);
//        System.out.println();
//        System.out.println(circleEq);

        var resData = new double[cableEq.getData().length + cabinetEq.getData().length + circleEq.getData().length];
        System.arraycopy(cableEq.getData(), 0, resData, 0, cableEq.getData().length);
        System.arraycopy(cabinetEq.getData(), 0, resData, cableEq.getData().length, cabinetEq.getData().length);
        System.arraycopy(circleEq.getData(), 0, resData, cableEq.getData().length + cabinetEq.getData().length, circleEq.getData().length);
        return (resData.length == 0)? new double[]{0} : resData;
    }

    private int getOrCreateIdx(Fuse fuse, HashMap<String, Integer> map, int[] last) {
        Integer idx = map.get(fuse.getName());
        if(idx == null) {
            last[0] = last[0] + 1;
            idx = last[0];
            map.put(fuse.getName(), idx);
        }
        return idx;
    }
}
