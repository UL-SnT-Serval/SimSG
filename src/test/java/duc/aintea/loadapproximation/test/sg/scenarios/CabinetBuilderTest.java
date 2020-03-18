package duc.aintea.loadapproximation.test.sg.scenarios;

import duc.aintea.loadapproximation.test.generator.Data;
import duc.aintea.sg.Cable;
import duc.aintea.sg.Fuse;
import duc.aintea.sg.Substation;
import duc.aintea.sg.scenarios.CabinetBuilder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CabinetBuilderTest extends ExtractionTest {
    private static Arguments[] generateFuseStatus() {
        return Data.generateRandomArrBooleans(6);
    }

    private static Arguments[] generateConsumptions() {
        return Data.generateRandomArrDoubles(3);
    }

    @ParameterizedTest
    @MethodSource("generateFuseStatus")
    public void testExtractionFuses(boolean[] fuseStates) {
        var def_consumptions = new double[3];
        generic_testCableExtraction(fuseStates, def_consumptions);
    }

    @ParameterizedTest
    @MethodSource("generateConsumptions")
    public void testExtractionCables(double[] consumptions) {
        var def_fuseStates = new boolean[]{true, true, true, true, true, true};
        generic_testCableExtraction(def_fuseStates, consumptions);
    }


    @Override
    protected Substation generateSubs(boolean[] fuseStates, double[] consumptions) {
        return CabinetBuilder.build(fuseStates, consumptions);
    }

    @Override
    protected Fuse[] extractFuses() {
        return CabinetBuilder.extractFuses(substation);
    }

    @Override
    protected Cable[] extractCables() {
        return CabinetBuilder.extractCables(substation);
    }
}