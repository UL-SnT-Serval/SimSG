package duc.sg.java.matrix.certain;

import duc.sg.java.cycle.all.InitAllCycleSubs;
import duc.sg.java.cycle.current.CycleDetection;
import duc.sg.java.matrix.certain.utils.Matrix;
import duc.sg.java.model.Cable;
import duc.sg.java.model.Entity;
import duc.sg.java.model.Fuse;
import duc.sg.java.model.Substation;

import java.util.*;

public class MatrixBuilder {
    private MatrixBuilder(){}

    public static FuseStatesMatrix build(Substation substation) {
        InitAllCycleSubs.init(substation);

        final var idxFuses = new HashMap<Fuse, Integer>();
        var idxLast = new int[]{-1};

        final var cableEq = new Matrix();
        final var cabinetEq = new Matrix();
        final var circleEq = new Matrix();

        var waitingList = new ArrayDeque<Entity>();
        var entityVisited = new HashSet<Entity>();
        var fuseInCircles = new HashSet<Fuse>();
        var paraCableFusesDone = new HashSet<>();

        var mapLineFuse = new ArrayList<Cable>();

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
                        if (!idxFuses.containsKey(fuse)) {
                            Fuse oppFuse = fuse.getOpposite();
                            cableEq.addLine();
                            mapLineFuse.add(fuse.getCable());
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
                            Fuse[] circle = CycleDetection.cycleFrom(substation, fuse);
                            if(circle.length != 0) {
                                Collections.addAll(fuseInCircles, circle);

                                Fuse fuseEnd = circle[0];
                                paraCableFusesDone.add(fuse);
                                paraCableFusesDone.add(fuseEnd);
                                int idxFuse = getOrCreateIdx(fuse, idxFuses, idxLast);
                                int idxFuseEnd = getOrCreateIdx(fuseEnd, idxFuses, idxLast);

                                circleEq.addLine();
                                setMatrix(circleEq,circleEq.getNumRows() - 1, idxFuse, 1);
                                setMatrix(circleEq,circleEq.getNumRows() - 1, idxFuseEnd, -1);
                            }
                        } else if(!entityVisited.contains(fuse.getOpposite().getOwner()) &&
                                !paraCableFusesDone.contains(fuse)) {
                            Optional<Fuse> oppFuse = getOtherFusePara(fuse, fuses);
                            if(oppFuse.isPresent()) {
                                Fuse fuseEnd = oppFuse.get();
                                paraCableFusesDone.add(fuse);
                                paraCableFusesDone.add(fuseEnd);

                                int idxFuse = getOrCreateIdx(fuse, idxFuses, idxLast);
                                int idxFuseEnd = getOrCreateIdx(fuseEnd, idxFuses, idxLast);

                                circleEq.addLine();
                                setMatrix(circleEq, circleEq.getNumRows() - 1, idxFuse, 1);
                                setMatrix(circleEq, circleEq.getNumRows() - 1, idxFuseEnd, -1);
                            }

                        }
                }
            }
        }

        if(circleEq.getNumCols() < cableEq.getNumCols()) {
            circleEq.addColumns(cableEq.getNumCols() - circleEq.getNumCols());
        }

        var resData = new double[cableEq.getData().length + cabinetEq.getData().length + circleEq.getData().length];
        System.arraycopy(cableEq.getData(), 0, resData, 0, cableEq.getData().length);
        System.arraycopy(cabinetEq.getData(), 0, resData, cableEq.getData().length, cabinetEq.getData().length);
        System.arraycopy(circleEq.getData(), 0, resData, cableEq.getData().length + cabinetEq.getData().length, circleEq.getData().length);
        resData = (resData.length == 0)? new double[]{0} : resData;

        return new FuseStatesMatrix(resData, cableEq.getNumCols(), idxFuses, mapLineFuse.toArray(new Cable[0]));
    }

    private static Optional<Fuse> getOtherFusePara(Fuse current, List<Fuse> entFuses) {
        if(entFuses.size() < 2) return Optional.empty();

        var otherEntity = current.getOpposite().getOwner();
        for (var fuse: entFuses) {
            if(!fuse.equals(current)) {
                var oppEntity = fuse.getOpposite().getOwner();
                if(oppEntity.equals(otherEntity)) {
                    return Optional.of(fuse);
                }
            }
        }

        return Optional.empty();
    }

    private static int getOrCreateIdx(Fuse fuse, HashMap<Fuse, Integer> map, int[] last) {
        map.computeIfAbsent(fuse, keyFuse -> ++last[0]);
        return map.get(fuse);
    }

    private static void setMatrix(Matrix matrix, int row, int column, double value) {
        if(row >= matrix.getNumRows()) {
            matrix.addLines(row - matrix.getNumRows() + 1);
        }

        if(column >= matrix.getNumCols()) {
            matrix.addColumns(column - matrix.getNumCols() + 1);
        }
        matrix.set(row, column, value);
    }
}