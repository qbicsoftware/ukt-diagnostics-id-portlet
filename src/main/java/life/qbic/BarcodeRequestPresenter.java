package life.qbic;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import life.qbic.helpers.Utils;

public class BarcodeRequestPresenter {
    private static Log log = LogFactoryUtil.getLog(MyPortletUI.class);

    private BarcodeRequestView barcodeRequestView;

    private BarcodeRequestModel barcodeRequestModel;

    private String[] patientSampleIdPair;

    private String user;

    public BarcodeRequestPresenter(BarcodeRequestView barcodeRequestView, BarcodeRequestModel barcodeRequestModel, String user) {
        this.barcodeRequestView = barcodeRequestView;
        this.barcodeRequestModel = barcodeRequestModel;
        this.user = user;
        giveLifeToElements();
    }

    private void giveLifeToElements() {

        barcodeRequestView.getPatientIdInputField().setInputPrompt("Choose patient");

        barcodeRequestView.getPatientIdInputField().addItems(barcodeRequestModel.getRegisteredPatients());


        barcodeRequestView.getTaskSelectionGroup().addValueChangeListener(value -> {
            if(barcodeRequestView.getTaskSelectionGroup().getValue().toString().contains("patient/sample")) {
                barcodeRequestView.getCreatePatientContainer().setVisible(true);
                barcodeRequestView.getCreateSampleContainer().setVisible(false);
            } else{
                barcodeRequestView.getCreatePatientContainer().setVisible(false);
                barcodeRequestView.getCreateSampleContainer().setVisible(true);
            }
        });

        barcodeRequestView.getPatentIdSampleIdButton().addClickListener(clickEvent -> {
            log.info(String.format("%s: Patient/Sample ID pair requested by user %s", AppInfo.getAppInfo(), this.user));
            barcodeRequestView.getPatentIdSampleIdButton().setEnabled(false);
            barcodeRequestView.getTaskSelectionGroup().setEnabled(false);
            Thread request = new RequestThread();
            request.start();
            UI.getCurrent().setPollInterval(50);
        });


        barcodeRequestView.getCreateSampleButton().addClickListener(clickEvent -> {
            log.info(String.format("%s: Additional DNA sample requested by user %s", AppInfo.getAppInfo(), this.user));
            barcodeRequestView.getCreateSampleButton().setEnabled(false);
            barcodeRequestView.getTaskSelectionGroup().setEnabled(false);
            Thread request = new NewSampleRequestThread();
            request.start();
            UI.getCurrent().setPollInterval(50);
        });

    }

    /**
     * An own thread for the heavy task of the barcode request and
     * sample registration
     */
    class RequestThread extends Thread {

        ProgressBar bar;
        HorizontalLayout container;
        Label loadingLabel;

        public RequestThread() {
            this.bar = barcodeRequestView.getSpinner();
            this.loadingLabel = barcodeRequestView.getLoadingLabel();
            this.container = barcodeRequestView.getSpinnerContainer();
            container.setVisible(true);
        }

        @Override
        public void run() {


            // Thread-safe UI access
            UI.getCurrent().access(() -> {
                container.setVisible(true);
                loadingLabel.setValue("Patient and Sample IDs are requested ...");
            });

            try {
                barcodeRequestModel.requestNewPatientSampleIdPair();
                patientSampleIdPair = barcodeRequestModel.getNewPatientSampleIdPair();
                barcodeRequestView.getPatientIdField().setValue(patientSampleIdPair[0]);
                barcodeRequestView.getSampleIdField().setValue(patientSampleIdPair[1]);
                barcodeRequestView.getPatientIdInputField().addItem(patientSampleIdPair[0]);
            } catch (Exception e) {
                UI.getCurrent().access(() ->
                    Utils.Notification("ID creation failed!", "No sample codes were created. Please try again.", "error"));
            }

            UI.getCurrent().access(() -> {
                container.setVisible(false);
                barcodeRequestView.getPatentIdSampleIdButton().setEnabled(true);
                barcodeRequestView.getTaskSelectionGroup().setEnabled(true);
            });

            // Stop polling
            UI.getCurrent().setPollInterval(-1);

        }
    }


    class NewSampleRequestThread extends Thread {
        ProgressBar bar;
        HorizontalLayout loadingInfoContainer;
        Label loadingLabel;

        public NewSampleRequestThread(){
            this.bar = barcodeRequestView.getSpinner();
            this.loadingLabel = barcodeRequestView.getLoadingLabel();
            this.loadingInfoContainer = barcodeRequestView.getSpinnerContainer();
            loadingInfoContainer.setVisible(true);
        }

        @Override
        public void run() {

            String patientID;

            if (barcodeRequestView.getPatientIdInputField().getValue() == null) {
                patientID =  "";
            } else {
                patientID =  barcodeRequestView.getPatientIdInputField().getValue().toString().trim();
            }

            log.debug("Selection: " + patientID);


            UI.getCurrent().access(()->
                loadingLabel.setValue("Check if patient ID is valid ..."));


            if (barcodeRequestView.getPatientIdInputField().getValue() == null ||
                    patientID.contains(" ")){
                UI.getCurrent().access(() -> Utils.Notification("Wrong/missing patient ID",
                        "Please enter a valid patient ID (like Q****ENTITY-1", "error"));
                log.error("Wrong or empty patient ID " + patientID);
                barcodeRequestView.getPatientIdInputField().addStyleName("textfield-red");
            } else {
                if(!barcodeRequestModel.checkIfPatientExists(patientID)){
                    loadingLabel.setValue("No patient with that ID was found.");

                    UI.getCurrent().access(() ->
                        Utils.Notification("Patient ID does not exist yet.",
                                "Please request a new patient/sample pair (Selection 1)", "error"));
                    log.error("Patient with ID " + patientID + " could not be found!");
                    barcodeRequestView.getPatientIdInputField().addStyleName("textfield-red");
                } else {
                    loadingLabel.setValue("Patient found, creating and registering new sample ...");
                    barcodeRequestView.getPatientIdInputField().removeStyleName("textfield-red");
                    String sampleCode = barcodeRequestModel.addNewSampleToPatient(patientID, "tumor tissue");
                    if(sampleCode.isEmpty()){
                        Utils.Notification("Sample registration error",
                                "Please contact us via helpdesk@qbic.uni-tuebingen.de",
                                "error");
                    } else{
                        log.info(String.format("Determined new sample barcode for patient %s: %s", patientID, sampleCode));
                        barcodeRequestView.getNewSampleIdField().setValue(sampleCode);
                    }
                }
            }

            UI.getCurrent().access(() -> {
                loadingInfoContainer.setVisible(false);
                barcodeRequestView.getCreateSampleButton().setEnabled(true);
                barcodeRequestView.getTaskSelectionGroup().setEnabled(true);
            });

            UI.getCurrent().setPollInterval(-1);

        }


    }
}