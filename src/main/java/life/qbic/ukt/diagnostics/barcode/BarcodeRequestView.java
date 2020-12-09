package life.qbic.ukt.diagnostics.barcode;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.*;


public interface BarcodeRequestView {

    /**
     * Getter for the patientID text field
     * @return text field
     */
    Label getPatientIdField();

    /**
     * Getter for the sampleID text field
     * @return text field
     */
    Label getSampleIdField();


    /**
     * Getter for the radio button group
     * @return text field
     */
    RadioButtonGroup<String> getTaskSelectionGroup();


    /**
     * Getter for the total view elements
     * container
     * @return vertical layout
     */
    VerticalLayout getFullView();


    /**
     * Getter for the patient Id / sample Id
     * request button
     * @return the button instance
     */
    Button getPatentIdSampleIdButton();


    /**
     * Getter for the loading spinner
     * @return The spinner
     */
    ProgressBar getSpinner();

    /**
     * Getter for the loading label
     * @return The loading label
     */
    Label getLoadingLabel();

    /**
     * Get the container for spinner and label
     * @return The container layout
     */
    HorizontalLayout getSpinnerContainer();

    /**
     * Getter for the CreatePatientContainer
     * @return The container
     */
    VerticalLayout getCreatePatientContainer();

    /**
     * Getter for the CreateSampleContainer
     * @return The container
     */
    VerticalLayout getCreateSampleContainer();

    /**
     * Getter for the create sample button
     * @return The button
     */
    Button getCreateSampleButton();

    /**
     * Getter for the patient ID input field
     * @return The combo box
     */
    ComboBox<String> getPatientIdInputField();

    /**
     * Getter for the patient ID input field data provider
     * @return The data provider
     */
    ListDataProvider<String> getPatientIdDataProvider();

    /**
     * Getter for the sample ID field
     * @return The label
     */
    Label getNewSampleIdField();

}
