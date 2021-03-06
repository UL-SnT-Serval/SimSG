package creos.simsg.api.matrix.certain;

import creos.simsg.api.model.Configuration;
import creos.simsg.api.model.Entity;
import creos.simsg.api.model.Fuse;
import creos.simsg.api.model.Substation;
import creos.simsg.api.circlefinder.Circle;
import creos.simsg.api.circlefinder.CircleFinder;
import creos.simsg.api.circlefinder.CircleUtils;
import creos.simsg.api.matrix.EquationMatrix;
import creos.simsg.api.matrix.MatrixBuilder;
import creos.simsg.api.model.*;
import creos.simsg.api.navigation.Actionner;
import creos.simsg.api.navigation.Condition;
import creos.simsg.api.navigation.bfs.BFSEntity;
import creos.simgsg.api.utils.ListConverter;
import creos.simgsg.api.utils.MatrixDouble;

import java.util.*;

/**
 * Computes the matrix builder based on a mono-substation topology.
 * This algorithm does not consider uncertainty of fuses.
 */
public class CertainMatrixBuilder implements MatrixBuilder {
    private Map<Fuse, Integer> idxFuses;
    private int[] idxLast;
    private MatrixDouble equations;
    private Set<Circle> processedCircle;
    private List<Double> eqResults;

    private int getOrCreateIdx(Fuse fuse, Map<Fuse, Integer> map, int[] last) {
        map.computeIfAbsent(fuse, keyFuse -> ++last[0]);
        return map.get(fuse);
    }

    private void init() {
        idxFuses = new HashMap<>();
        idxLast = new int[]{-1};
        equations = new MatrixDouble();
        processedCircle = new HashSet<>();
        eqResults = new ArrayList<>();
    }

    @Override
    public EquationMatrix[] build(Substation substation, Configuration configuration) {
        init();

        CircleFinder.getDefault().getCircles(substation);

        Actionner<Entity> actionner = (Entity currEntity) -> {
            final List<Fuse> closedFuses = configuration.getClosedFuses(currEntity);

            if (!(currEntity instanceof Substation) && closedFuses.size() > 1) {
                equations.addLine();
                eqResults.add(0.);
            }
            int rowCabEq = equations.getNumRows() - 1;

            for (Fuse fuse : closedFuses) {
                cableEq(configuration, fuse);
                cabinetEq(currEntity, closedFuses, rowCabEq, fuse);
                circleEq(substation, configuration, fuse);
            }
        };

        Condition condition = (Fuse currFuse) -> {
            Fuse oppFuse = currFuse.getOpposite();
            Entity oppEntity = oppFuse.getOwner();
            return configuration.isClosed(currFuse) &&
                    configuration.isClosed(oppFuse) &&
                    !configuration.isDeadEnd(oppEntity);
        };

        BFSEntity.INSTANCE.navigate(substation, actionner, condition);

        var resData = (equations.getData().length == 0)? new double[]{0} : equations.getData();
        EquationMatrix res = new EquationMatrixImp(resData, idxFuses, ListConverter.convert(eqResults));
        return new EquationMatrix[]{res};
    }

    private void circleEq(Substation substation, Configuration configuration, Fuse fuse) {
        List<Circle> circles = CircleUtils.circlesWith(substation, fuse);
        circles.forEach((Circle circle) -> {
            if(circle.isEffective(configuration) && !processedCircle.contains(circle)) {
                processedCircle.add(circle);
                Fuse end = circle.getOtherEndPoint(fuse);
                addCircleEq(fuse, end);
            }
        });
    }

    private void cabinetEq(Entity currEntity, List<Fuse> fuses, int rowCabEq, Fuse fuse) {
        if (!(currEntity instanceof Substation) && fuses.size() > 1) {
            int idxFuse = getOrCreateIdx(fuse, idxFuses, idxLast);
            equations.set( rowCabEq, idxFuse, 1);
        }
    }

    private void cableEq(Configuration configuration, Fuse fuse) {
        if (!idxFuses.containsKey(fuse)) {
            addCableEq(configuration, fuse);
        }
    }

    private void addCircleEq(Fuse fuse, Fuse fuseEnd) {
        int idxFuse = getOrCreateIdx(fuse, idxFuses, idxLast);
        int idxFuseEnd = getOrCreateIdx(fuseEnd, idxFuses, idxLast);

        equations.addLine();
        eqResults.add(0.);
        equations.set(equations.getNumRows() - 1, idxFuse, 1);
        equations.set(equations.getNumRows() - 1, idxFuseEnd, -1);
    }

    private void addCableEq(Configuration configuration, Fuse fuse) {
        Fuse oppFuse = fuse.getOpposite();
        equations.addLine();
        eqResults.add(fuse.getCable().getConsumption());
        int idxFuse = getOrCreateIdx(fuse, idxFuses, idxLast);
        equations.set(equations.getNumRows() - 1, idxFuse, 1);

        if (configuration.isClosed(oppFuse) && !configuration.isDeadEnd(oppFuse.getOwner())) {
            int idxOpp = getOrCreateIdx(oppFuse, idxFuses, idxLast);
            equations.set( equations.getNumRows() - 1, idxOpp, 1);
        }
    }

}
