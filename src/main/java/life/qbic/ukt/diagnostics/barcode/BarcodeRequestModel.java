package life.qbic.ukt.diagnostics.barcode;

import java.util.Collection;
import java.util.List;

public interface BarcodeRequestModel {

    /**
     * This method will request a new patient
     * and sample ID pair from openBIS and
     * return it in a String array with
     */
    void requestNewPatientSampleIdPair();

    /**
     * This method will return you the ID pair, after you submit the request
     * in the first place.
     * @return An array with the patient ID and sample ID
     *     array[0] => patient ID
     *     array[1] => sample ID
     */
    String[] getNewPatientSampleIdPair();

    /**
     * Make a request to openBIS with a Q_BIOLOGICAL_ENTITY
     * ID
     * @return True, if sample with ID exists, else false
     */
    boolean checkIfPatientExists(String sampleID);

    /**
     * Tries to register a sample (TEST_SAMPLE) in openBIS and returns the
     * barcode, if registration was successful
     * @param patientID The QBiC patient ID
     * @param filterProperty Filters BIOLOGICAL SAMPLE for a property value!
     * @return A valid sample ID, an empty string if registration
     * did not work
     */
    String addNewSampleToPatient(String patientID, String filterProperty);

    /**
     * Querries all registered patient IDs
     * @return A collection of patient IDs
     */
    List<String> getRegisteredPatients();
}
