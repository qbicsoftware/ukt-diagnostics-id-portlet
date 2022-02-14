package life.qbic;

import javax.portlet.PortletContext;
import javax.portlet.PortletSession;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.WrappedPortletSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.portal.utils.ConfigurationManager;
import life.qbic.portal.utils.ConfigurationManagerFactory;
import life.qbic.portal.utils.PortalUtils;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Theme("mytheme")
@SuppressWarnings("serial")
public class MyPortletUI extends UI {

    private static Log log = LogFactoryUtil.getLog(MyPortletUI.class);

    private static Boolean testing = true;

    @Override
    protected void init(VaadinRequest request) {
        UI.getCurrent().setPollInterval( -1 );
        String portletContextName = "Testing";
        Integer numOfRegisteredUsers = 1;
        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        setContent(layout);

        String userID = "MISSING SCREENNAME";
        if (PortalUtils.isLiferayPortlet()) {
            userID = PortalUtils.getUser().getScreenName();
            portletContextName = getPortletContextName(request);
            testing = false;
        }

        String[] credentials = getCredentials();
        OpenBisSession obisSession = new OpenBisSession(credentials[2], credentials[0], credentials[1]);

        OpenBisClient openBisClient = makeOpenBisClient();

        if (obisSession.token == null){
            showNotiffication("Could not initialize connection to openBIS", Notification.Type.ERROR_MESSAGE);
            layout.addComponent(new Label("<h1>Error</h1>Something went wrong, please contact: helpdesk@qbic.uni-tuebingen.de", ContentMode.HTML));
            return;
        }

        final BarcodeRequestView requestView = new BarcodeRequestViewImpl();
        final BarcodeRequestModel barcodeRequestModel = new BarcodeRequestModelImpl(obisSession, openBisClient);
        final BarcodeRequestPresenter barcodeRequestPresenter = new BarcodeRequestPresenter(requestView, barcodeRequestModel, userID);

        layout.addComponent(requestView.getFullView());

    }

    /**
     * Create, check and return openBisClient object
     * @return OpenbisClient
     */
    private OpenBisClient makeOpenBisClient(){

        String[] credentials = getCredentials();
        if (credentials == null){
            return null;
        }
        OpenBisClient openBisClient = new OpenBisClient(credentials[0], credentials[1], credentials[2]);
        try{
            openBisClient.login();
        } catch (Exception exc){
            log.error(exc);
            return null;
        }

        return openBisClient;
    }


    /**
     * Acquire the credentials from local properties file or from liferay
     * @return An array with the credentials
     *      String[0] = loginID
     *      String[1] = password
     *      String[2] = serverURL
     */
    private String[] getCredentials() {
        final String[] credentials = new String[3];

        if (testing){
            try{
                final BufferedReader propertiesFile = Files.newBufferedReader(Paths.get("/etc/openbis.properties"));
                final Properties prop = new Properties();
                prop.load(propertiesFile);
                credentials[0] = prop.getProperty("openbisuser");
                credentials[1] = prop.getProperty("openbispw");
                credentials[2] = prop.getProperty("openbisURI");
            } catch (Exception exc){
                log.error(exc);
                return null;
            }
        } else {
            final ConfigurationManager config = ConfigurationManagerFactory.getInstance();
            credentials[0] = config.getDataSourceUser();
            credentials[1] = config.getDataSourcePassword();
            credentials[2] = config.getDataSourceApiUrl();
        }

        return credentials;

    }

    /**
     * Get the portlet's context name
     * @param request The vaadin request
     * @return The context name of the portlet
     */
    private String getPortletContextName(VaadinRequest request) {
        WrappedPortletSession wrappedPortletSession = (WrappedPortletSession) request
                .getWrappedSession();
        PortletSession portletSession = wrappedPortletSession
                .getPortletSession();

        final PortletContext context = portletSession.getPortletContext();
        final String portletContextName = context.getPortletContextName();
        return portletContextName;
    }

    /**
     * Small wrapper for notifications
     * @param message the message
     * @param type the message type
     */
    private void showNotiffication(String message, Notification.Type type){
        Notification note = new Notification(message, type);
        note.show(Page.getCurrent());

    }

}
