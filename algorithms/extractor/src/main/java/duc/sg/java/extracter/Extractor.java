package duc.sg.java.extracter;

import duc.sg.java.model.Substation;

import java.util.List;

/**
 * An extractor is an algorithm that navigates through the topology, and collect (extract) elements with the generic
 * type.
 *
 * @param <T> Type of the extracted elements
 */
public interface Extractor<T> {
    void extractAndSave(Substation substation);
    List<T> getExtracted(Substation substation);
}
