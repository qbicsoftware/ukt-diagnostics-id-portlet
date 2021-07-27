package life.qbic;

import ch.ethz.sis.openbis.generic.asapi.v3.IApplicationServerApi;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.operation.IOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.common.search.SearchResult;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.entitytype.id.EntityTypePermId;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.create.ExperimentCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.experiment.id.ExperimentIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.operation.SynchronousOperationExecutionOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.project.id.ProjectIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.Sample;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.CreateSamplesOperation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.create.SampleCreation;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.fetchoptions.SampleFetchOptions;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.id.SampleIdentifier;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.sample.search.SampleSearchCriteria;
import ch.ethz.sis.openbis.generic.asapi.v3.dto.space.id.SpacePermId;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import life.qbic.helpers.BarcodeFunctions;
import life.qbic.helpers.Utils;
import life.qbic.openbis.openbisclient.OpenBisClient;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.groovy.json.internal.IO;

import static life.qbic.helpers.BarcodeFunctions.checksum;

public class BarcodeRequestModelImpl implements BarcodeRequestModel{

    private final OpenBisSession obisSession;

    private final OpenBisClient openBisClient;

    private final static String SPACE = "UKT_DIAGNOSTICS";

    private final static String PROJECTID = "/UKT_DIAGNOSTICS/QUK17";

    private final static String CODE = "QUK17";

    private static Log log = LogFactoryUtil.getLog(MyPortletUI.class);

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVW".toCharArray();

    private static String[] patientSampleIdPair= null;

    public BarcodeRequestModelImpl(OpenBisSession session, OpenBisClient client){

        this.obisSession = session;
        this.openBisClient = client;
    }

    @Override
    public void requestNewPatientSampleIdPair() {
        patientSampleIdPair = new String[2];

        // In case registration fails, return null
        int retryCount = 7;
        int[] sizes = getNumberOfSampleTypes();

        boolean patientRegistered = false;
        boolean samplesRegistered = false;

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            if (attempt == retryCount) {
                log.error("Max number of registration attempts tried but is still failing.");
                throw new RuntimeException("Registration failed.");
            }
            // offset is +2, because there is always an attachment sample per project
            String biologicalSampleCodeBlood = createBarcodeFromInteger(sizes[0] + 1 );
            String biologicalSampleCodeTumor = createBarcodeFromInteger(sizes[0] + 2 );

            // offset is +3, because of the previous created sample and the attachement
            String testSampleCodeBlood = createBarcodeFromInteger(sizes[0] + 3);
            String testSampleCodeTumor = createBarcodeFromInteger(sizes[0] + 4);
            String patientId = patientRegistered ? CODE + "ENTITY-" + (sizes[1]): CODE + "ENTITY-" + (sizes[1]+1) ;

            patientSampleIdPair[0] = patientId;
            patientSampleIdPair[1] = testSampleCodeTumor;

            // Logging code block
            log.debug(String.format("Number of non-entity samples: %s", sizes[0]));
            if (!patientRegistered && !registerPatientOnly(patientId)) {
                // We increase the patient number if the registration failed and try again
                sizes[1] = sizes[1] + 1;
            } else {
                patientRegistered = true;
                // This should be sufficient given the current usage ratio to avoid sample
                // registration failures due to to many parallel requests.
                int sampleRegistrationAttempts = 100;
                int currentAttempt = 0;
                System.out.printf("Trying to register samples for patient %s%n", patientId);
                while (!registerSamplesForPatient(patientId, biologicalSampleCodeTumor, biologicalSampleCodeBlood,
                    testSampleCodeTumor, testSampleCodeBlood)) {
                    if(currentAttempt == sampleRegistrationAttempts) {
                        throw new RuntimeException(String.format("Sample registration for patient %s failed.", patientId));
                    }
                    // We try the next sample increment
                    sizes[0] = sizes[0] + 1;
                    // offset is +2, because there is always an attachment sample per project
                    biologicalSampleCodeBlood = createBarcodeFromInteger(sizes[0] + 1 );
                    biologicalSampleCodeTumor = createBarcodeFromInteger(sizes[0] + 2 );

                    // offset is +3, because of the previous created sample and the attachement
                    testSampleCodeBlood = createBarcodeFromInteger(sizes[0] + 3);
                    testSampleCodeTumor = createBarcodeFromInteger(sizes[0] + 4);
                    currentAttempt++;
                }
                patientSampleIdPair[1] = testSampleCodeTumor;
                samplesRegistered = true;
            }

            if (patientRegistered && samplesRegistered){
                log.info(String.format("%s: New patient ID created %s", AppInfo.getAppInfo(), patientSampleIdPair[0]));
                log.info(String.format("%s: New tumor sample ID created %s", AppInfo.getAppInfo(), patientSampleIdPair[1]));
                log.info(String.format("%s: New tumor DNA sample ID created %s", AppInfo.getAppInfo(), biologicalSampleCodeTumor));
                log.info(String.format("%s: New blood sample ID created %s", AppInfo.getAppInfo(), biologicalSampleCodeBlood));
                log.info(String.format("%s: New blood DNA sample ID created %s", AppInfo.getAppInfo(), testSampleCodeBlood));
                break;
            }

            try {
                Thread.sleep(1000);
            } catch ( InterruptedException ignored) {}
            }

    }


    /**
     * Determines the number of non entity samples and
     * total samples for a project
     * @return An 1D array with 2 entries:
     *          array[0] = number of non entities
     *          array[1] = number of total entities
     */
    private int[] getNumberOfSampleTypes(){
        int[] sizes = new int[2];
        List<Sample> sampleList = this.getSamplesOfProject(CODE);
        List<Sample> entities = getEntities(sampleList);

        String highestID = null;
        // Filter sample list for Q_BIOLOGICAL_SAMPLE and Q_TEST_SAMPLE
        for(Sample s : sampleList){
            if (s.getType().getCode().equals("Q_BIOLOGICAL_SAMPLE") || s.getType().getCode().equals("Q_TEST_SAMPLE")){
                String idCount = s.getCode().substring(5, 9);
                if (highestID == null){
                    highestID = idCount;
                } else {
                    if (isIdHigher(highestID, idCount))
                        highestID = idCount;
                }
            }
        }
        log.info(convertIdToInt(highestID));
        sizes[0] = convertIdToInt(highestID);
        sizes[1] = entities.size();
        return sizes;
    }

    private List<Sample> getSamplesOfProject(String id) {
        IApplicationServerApi apiConnection = obisSession.api;
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withCode().thatContains(id);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();

        SearchResult<Sample> result = apiConnection.searchSamples(obisSession.token, criteria, fetchOptions);
        log.info("Found "+ result.getObjects().size() + " samples for project " + id + ".");
        return result.getObjects();
    }

    private boolean isIdHigher(String idA, String idB){

        // Compare the letter first
        // eg: B < Z 
        if (idB.toCharArray()[3] > idA.toCharArray()[3])
            return true;
        // B < Z
        if (idB.toCharArray()[3] < idA.toCharArray()[3])
            return false;
        // Compare the digits
        // eg. 002 > 001
        if (Integer.parseInt(idB.substring(0,3)) > Integer.parseInt(idA.substring(0,3)))
            return true;
        return false;
    }

    private int convertIdToInt(String id){
        int iterator = Integer.parseInt(id.substring(0, 3));
        char idChar = id.charAt(3);

        int multiplier = 0;
        for (int i=0; i<ALPHABET.length; i++){
            if (idChar == ALPHABET[i])
                multiplier = i;
        }
        return multiplier * 1000 + iterator;  
    }

    @Override
    public String[] getNewPatientSampleIdPair() {
        return patientSampleIdPair;
    }

    @Override
    public boolean checkIfPatientExists(String sampleID) {
        Sample sample;
        try{
            IApplicationServerApi apiConnection = obisSession.api;
            SampleSearchCriteria criteria = new SampleSearchCriteria();
            criteria.withCode().thatEquals(sampleID);

            SampleFetchOptions fetchOptions = new SampleFetchOptions();
            fetchOptions.withType();

            SearchResult<Sample> result = apiConnection.searchSamples(obisSession.token, criteria, fetchOptions);
            sample = result.getObjects().get(0);
        } catch (Exception exc){
            log.error(exc);
            return false;
        }
        return sample != null;
    }

    @Override
    public String addNewSampleToPatient(String patientID, String filterProperty) {

        IApplicationServerApi apiConnection = obisSession.api;
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withCode().thatEquals(patientID);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withChildren().withProperties();
        fetchOptions.withProperties();
        fetchOptions.withChildren().withType();
        fetchOptions.withType();

        SearchResult<Sample> result = apiConnection.searchSamples(obisSession.token, criteria, fetchOptions);
        List<Sample> childrenList = result.getObjects().get(0).getChildren();

        if (childrenList.size() == 0){
            log.error(String.format("Sample list was empty, patient ID %s seems to have no parents or childrens", patientID));
            return "";
        }

        List<Sample> biologicalSamplesOnly = childrenList.stream().filter(sample -> sample.getType().getCode()
                .equals("Q_BIOLOGICAL_SAMPLE"))
                .collect(Collectors.toList())
                .stream().filter(sample -> sample.getProperties().containsValue(filterProperty)).collect(Collectors.toList());

        int size = biologicalSamplesOnly.size();

        if (size == 0){
            log.error(String.format("No samples of type 'Q_BIOLOGICAL_SAMPLE' found for patient ID %s.", patientID));
            return "";
        } else if (size > 1){
            log.error(String.format("More than 1 sample of type 'Q_BIOLOGICAL_SAMPLE' found for patient ID %s.", patientID));
            return "";
        }

        int existingSamples = getNumberOfSampleTypes()[0];

        String sampleBarcode = createBarcodeFromInteger(existingSamples + 1);

        if (sampleBarcode.isEmpty()){
            log.error("Retrieval of a new sample barcode failed. No new sample for an existing patient " +
                    "was created.");
            return "";
        }

        int maxAttempts = 100;
        int attempt = 0;
        boolean registrationSuccessful = false;

        while (attempt <= maxAttempts && !registrationSuccessful) {
            sampleBarcode = createBarcodeFromInteger(existingSamples + 1);
            IOperation op = registerTestSample(sampleBarcode, "/" + SPACE + "/" + biologicalSamplesOnly.get(0).getCode());
            try {
                handleOperations(Arrays.asList(op));
                registrationSuccessful = true;
            } catch (RuntimeException e) {
                log.error(String.format("Could not create and add new sample to patient %s!", patientID), e);
            }
            attempt++;
            existingSamples++;
        }

        if (attempt == maxAttempts) {
            // that means no successful sample creation happened, we dont return
            // any sample id to the user
            sampleBarcode = "";
        }

        return sampleBarcode;
    }

    @Override
    public List<String> getRegisteredPatients() {
        IApplicationServerApi apiConnection = obisSession.api;
        SampleSearchCriteria criteria = new SampleSearchCriteria();
        criteria.withCode().thatContains(CODE);

        SampleFetchOptions fetchOptions = new SampleFetchOptions();
        fetchOptions.withType();

        SearchResult<Sample> result = apiConnection.searchSamples(obisSession.token, criteria, fetchOptions);

        return result.getObjects().stream().filter(sample -> sample.getType().getCode()
                .equals("Q_BIOLOGICAL_ENTITY"))
                .map(Sample::getCode)
                .collect(Collectors.toList());
    }

    private boolean registerPatientOnly(String patientId) {
        IOperation patientOp = registerEntity(patientId);
        List<IOperation> operations = Arrays.asList(patientOp);
        try {
            handleOperations(operations);
        } catch (RuntimeException e) {
            log.error(String.format("Registration of patient only for %s failed!", patientId), e);
            return false;
        }
        return true;
    }

    /**
     * Registration of a new patient with samples
     * @param patientId A code for the sample type Q_BIOLOGICAL_ENTITY
     * @param biologicalSampleCodeBlood A code for the sample type Q_BIOLOGICAL_SAMPLE (Blood)
     * @param biologicalSampleCodeTumor A code for the sample type Q_BIOLOGICAL_SAMPLE (Tumor)
     * @param testSampleCodeBlood A code for the sample type Q_TEST_SAMPLE (Blood)
     * @param testSampleCodeTumor A code for the sample type Q_TEST_SAMPLE (Tumor)
     * @return True, if registration was successful, else false
     */
    private boolean registerSamplesForPatient(String patientId, String biologicalSampleCodeTumor, String biologicalSampleCodeBlood,
                                              String testSampleCodeTumor, String testSampleCodeBlood) {

        IOperation biologicalSampleCodeTumorOp = registerBioSample(biologicalSampleCodeTumor, "/"+SPACE+"/"+patientId, "tumor tissue");
        IOperation testSampleCodeTumorOp = registerTestSample(testSampleCodeTumor, "/"+SPACE+"/"+biologicalSampleCodeTumor);
        IOperation biologicalSampleCodeNormalOp = registerBioSample(biologicalSampleCodeBlood, "/"+SPACE+"/"+patientId, "blood");
        IOperation testSampleCodeBloodOp = registerTestSample(testSampleCodeBlood, "/"+SPACE+"/"+biologicalSampleCodeBlood);

        List<IOperation> operations = Arrays.asList(
            biologicalSampleCodeNormalOp,
            biologicalSampleCodeTumorOp,
            testSampleCodeTumorOp,
            testSampleCodeBloodOp);
        try {
            handleOperations(operations);
        } catch (RuntimeException e) {
            log.error("Registration failed!", e);
            return false;
        }
        return true;
    }

    /**
     * Registration of a new test sample
     * @param testSampleCode A code for the test sample type Q_TEST_SAMPLE
     * @param parent A code for the parent sample type Q_BIOLOGICAL_SAMPLE
     * @return an operation, if registration was successful
     */
    private IOperation registerTestSample(String testSampleCode, String parent) {

        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setTypeId(new EntityTypePermId("Q_TEST_SAMPLE"));
        sampleCreation.setSpaceId(new SpacePermId(SPACE));
        List<SampleIdentifier> parents = new ArrayList<>();
        parents.add(new SampleIdentifier(parent));
        sampleCreation.setParentIds(parents);
        sampleCreation.setExperimentId(new ExperimentIdentifier(String.format("/%s/%s/%s%s", SPACE, CODE, CODE, "E3")));
        sampleCreation.setCode(testSampleCode);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("Q_SAMPLE_TYPE", "DNA");
        sampleCreation.setProperties(metadata);

        List<SampleCreation> registrationApplication = new ArrayList<>();
        registrationApplication.add(sampleCreation);

        IOperation operation = new CreateSamplesOperation(registrationApplication);
        return operation;
    }

    private void handleOperations(List<IOperation> operations) throws RuntimeException {
        SynchronousOperationExecutionOptions executionOptions = new SynchronousOperationExecutionOptions();
        try {
            openBisClient.getV3().executeOperations(openBisClient.getSessionToken(), operations, executionOptions);
        } catch (Exception e) {
            log.error("Registration failed for openBIS operation.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Registration of a new biological sample
     * @param biologicalSampleCode A code for the sample type Q_BIOLOGICAL_SAMPLE
     * @param parent A code for the parent sample type Q_BIOLOGICAL_ENTITY
     * @return an operation, if registration was successful, else false
     */
    private IOperation registerBioSample(String biologicalSampleCode, String parent, String tissue) {
        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setTypeId(new EntityTypePermId("Q_BIOLOGICAL_SAMPLE"));
        sampleCreation.setSpaceId(new SpacePermId(SPACE));
        List<SampleIdentifier> parents = new ArrayList<>();
        parents.add(new SampleIdentifier(parent));
        sampleCreation.setParentIds(parents);
        sampleCreation.setExperimentId(new ExperimentIdentifier(String.format("/%s/%s/%s%s", SPACE, CODE, CODE, "E2")));
        sampleCreation.setCode(biologicalSampleCode);

        Map<String, String> metadata = new HashMap<>();
        if (tissue.equals("blood")){
            metadata.put("Q_PRIMARY_TISSUE", "PBMC");
        } else {
            metadata.put("Q_PRIMARY_TISSUE", "TUMOR_TISSUE_UNSPECIFIED");
        }
        metadata.put("Q_TISSUE_DETAILED", tissue);
        sampleCreation.setProperties(metadata);

        List<SampleCreation> registrationApplication = new ArrayList<>();
        registrationApplication.add(sampleCreation);

        IOperation operation = new CreateSamplesOperation(registrationApplication);
        return operation;
    }

    /**
     * Registration of a new test sample
     * @param patientId A code for the sample type Q_BIOLOGICAL_ENTITY
     * @return an operation, if registration was successful, else false
     */
    private IOperation registerEntity(String patientId){
        SampleCreation sampleCreation = new SampleCreation();
        sampleCreation.setTypeId(new EntityTypePermId("Q_BIOLOGICAL_ENTITY"));
        sampleCreation.setSpaceId(new SpacePermId(SPACE));
        sampleCreation.setExperimentId(new ExperimentIdentifier(String.format("/%s/%s/%s%s", SPACE, CODE, CODE, "E1")));
        sampleCreation.setCode(patientId);

        Map<String, String> metadata = new HashMap<>();
        //Human
        metadata.put("Q_NCBI_ORGANISM", "9606");
        sampleCreation.setProperties(metadata);

        List<SampleCreation> registrationApplication = new ArrayList<>();
        registrationApplication.add(sampleCreation);

        IOperation operation = new CreateSamplesOperation(registrationApplication);
        return operation;
    }


    /**
     * Get a sample list with samples from type
     * 'Q_BIOLOGICAL_ENTITY' from a list of samples
     * @param sampleList The sample list to be filtered
     * @return The filtered list
     */
    private List<Sample> getEntities(List<Sample> sampleList){
        List<Sample> filteredList = new ArrayList<>();

        for(Sample s : sampleList){
            if (s.getType().getCode().equals("Q_BIOLOGICAL_ENTITY")){
                filteredList.add(s);
            }
        }

        return filteredList;

    }

    /**
     * Creates a complete barcode from a given number using
     * the global project code prefix.
     * Checksum calculation and barcode verification is included.
     * @param number An integer number
     * @return A fully formatted valid QBiC barcode
     */
    private String createBarcodeFromInteger(int number){
        int multiplicator = number / 1000;
        char letter = ALPHABET[multiplicator];

        int remainingCounter = number - multiplicator*1000;

        if (remainingCounter > 999 || remainingCounter < 0){
            return "";
        }

        String preBarcode = CODE + Utils.createCountString(remainingCounter, 3) + letter;

        String barcode = preBarcode + checksum(preBarcode);

        if (!BarcodeFunctions.isQbicBarcode(barcode)){
            log.error(String.format("%s: Barcode created from Integer is not a valid barcode: %s", AppInfo.getAppInfo(),
                    barcode));
            barcode = "";
        }

        return barcode;

    }

}
