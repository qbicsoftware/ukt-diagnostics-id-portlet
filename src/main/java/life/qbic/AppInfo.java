package life.qbic;

public class AppInfo {

    public static final String APPNAME = "ukt_diagnostic_portlet";

    public static final String VERSION = "v1.2.8";

    public static String getAppInfo(){
        return String.format("[%s-%s]", APPNAME, VERSION);
    }

}
