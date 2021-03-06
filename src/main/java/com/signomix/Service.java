/**
 * Copyright (C) Grzegorz Skorupa 2018.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix;

import com.signomix.event.AlertApiEvent;
import com.signomix.event.UplinkEvent;
import com.signomix.in.http.ActuatorApi;
import com.signomix.in.http.KpnApi;
import com.signomix.in.http.LoRaApi;
import com.signomix.in.http.TtnApi;
import com.signomix.iot.IotData;
import com.signomix.event.IotEvent;
import com.signomix.event.MailingApiEvent;
import com.signomix.event.NewDataEvent;
import com.signomix.event.SystemEvent;
import com.signomix.out.db.ActuatorCommandsDBIface;
import com.signomix.out.db.IotDataStorageIface;
import com.signomix.out.db.IotDatabaseIface;
import com.signomix.out.db.ShortenerDBIface;
import org.cricketmsf.Event;
import org.cricketmsf.Kernel;
import org.cricketmsf.RequestObject;
import java.util.HashMap;
import org.cricketmsf.annotation.HttpAdapterHook;
import org.cricketmsf.in.http.HtmlGenAdapterIface;
import org.cricketmsf.in.http.ParameterMapResult;
import org.cricketmsf.in.http.StandardResult;
import org.cricketmsf.in.scheduler.SchedulerIface;
import org.cricketmsf.microsite.cms.CmsIface;
import org.cricketmsf.microsite.out.auth.AuthAdapterIface;
import com.signomix.out.iot.ThingsDataIface;
import org.cricketmsf.microsite.out.queue.QueueAdapterIface;
import org.cricketmsf.microsite.out.user.UserAdapterIface;
import org.cricketmsf.microsite.out.user.UserException;
import org.cricketmsf.microsite.user.User;
import org.cricketmsf.out.db.KeyValueDBIface;
import org.cricketmsf.out.file.FileReaderAdapterIface;
import org.cricketmsf.out.log.LoggerAdapterIface;
import com.signomix.out.gui.DashboardAdapterIface;
import com.signomix.out.iot.ActuatorDataIface;
import com.signomix.out.iot.Alert;
import com.signomix.out.iot.Device;
import com.signomix.out.iot.ThingsDataException;
import com.signomix.out.mailing.MailingIface;
//import com.signomix.out.notification.EmailSenderIface;
import com.signomix.out.notification.NotificationIface;
import com.signomix.out.script.ScriptingAdapterIface;
import java.util.ArrayList;
import java.util.List;
import org.cricketmsf.annotation.EventHook;
import org.cricketmsf.annotation.PortEventClassHook;
import org.cricketmsf.event.EventMaster;
import org.cricketmsf.exception.EventException;
import org.cricketmsf.exception.InitException;
import org.cricketmsf.in.http.ResponseCode;
import org.cricketmsf.in.openapi.OpenApiIface;
import org.cricketmsf.microsite.auth.AuthBusinessLogic;
import org.cricketmsf.microsite.cms.Document;
import org.cricketmsf.microsite.in.http.ContentRequestProcessor;
import org.cricketmsf.microsite.out.notification.EmailSenderIface;
import org.cricketmsf.microsite.out.queue.QueueException;
import org.cricketmsf.microsite.user.UserEvent;

/**
 * EchoService
 *
 * @author greg
 */
public class Service extends Kernel {

    public static String SIGNOMIX_TOKEN_NAME = "signomixToken";
    //service parameters
    Invariants invariants = null;

    // adapterClasses
    LoggerAdapterIface logAdapter = null;
    LoggerAdapterIface gdprLogger = null;
    KeyValueDBIface database = null;
    SchedulerIface scheduler = null;
    HtmlGenAdapterIface htmlAdapter = null;
    FileReaderAdapterIface fileReader = null;

    // optional
    // we don't need to register input adapters:
    // UserApi, AuthApi and other input adapter if we not need to acces them directly from the service
    //CM module
    KeyValueDBIface cmsDatabase = null;
    FileReaderAdapterIface cmsFileReader = null;
    CmsIface cms = null;
    //user module
    KeyValueDBIface userDB = null;
    UserAdapterIface userAdapter = null;
    //auth module
    KeyValueDBIface authDB = null;
    AuthAdapterIface authAdapter = null;
    //event broker client
    QueueAdapterIface queueAdapter = null;
    KeyValueDBIface queueDB = null;
    // IoT
    ThingsDataIface thingsAdapter = null;
    DashboardAdapterIface dashboardAdapter = null;
    ActuatorCommandsDBIface actuatorCommandsDB = null;
    ActuatorApi actuatorApi = null;
    ActuatorDataIface actuatorAdapter = null;
    //WidgetAdapterIface widgetAdapter = null;
    IotDatabaseIface thingsDB = null;
    IotDataStorageIface iotDataDB = null;

    ScriptingAdapterIface scriptingAdapter = null;
    //notifications and emails
    NotificationIface smtpNotification = null;
    NotificationIface smsNotification = null;
    NotificationIface pushoverNotification = null;
    NotificationIface slackNotification = null;
    NotificationIface telegramNotification = null;
    NotificationIface discordNotification = null;
    NotificationIface webhookNotification = null;
    EmailSenderIface emailSender = null;
    MailingIface mailingAdapter = null;

    //Integration services
    LoRaApi loraUplinkService = null;
    TtnApi ttnIntegrationService = null;
    KpnApi kpnUplinkService = null;

    //Utils
    ShortenerDBIface shortenerDB = null;
    OpenApiIface apiGenerator = null;

    public Service() {
        super();
    }

    @Override
    public void getAdapters() {
        // standard Cricket adapters
        logAdapter = (LoggerAdapterIface) getRegistered("logger");
        gdprLogger = (LoggerAdapterIface) getRegistered("GdprLogger");
        database = (KeyValueDBIface) getRegistered("database");
        scheduler = (SchedulerIface) getRegistered("scheduler");
        htmlAdapter = (HtmlGenAdapterIface) getRegistered("WwwService");
        fileReader = (FileReaderAdapterIface) getRegistered("FileReader");
        //cms
        cmsFileReader = (FileReaderAdapterIface) getRegistered("CmsFileReader");
        cmsDatabase = (KeyValueDBIface) getRegistered("cmsDB");
        cms = (CmsIface) getRegistered("cms");
        //user
        userAdapter = (UserAdapterIface) getRegistered("userAdapter");
        userDB = (KeyValueDBIface) getRegistered("userDB");
        //auth
        authAdapter = (AuthAdapterIface) getRegistered("authAdapter");
        authDB = (KeyValueDBIface) getRegistered("authDB");
        //queue
        queueDB = (KeyValueDBIface) getRegistered("queueDB");
        queueAdapter = (QueueAdapterIface) getRegistered("queueAdapter");
        //IoT
        thingsAdapter = (ThingsDataIface) getRegistered("iotAdapter");
        thingsDB = (IotDatabaseIface) getRegistered("iotDB");
        iotDataDB = (IotDataStorageIface) getRegistered("iotDataDB");
        dashboardAdapter = (DashboardAdapterIface) getRegistered("dashboardAdapter");
        actuatorCommandsDB = (ActuatorCommandsDBIface) getRegistered("actuatorCommandsDB");
        actuatorApi = (ActuatorApi) getRegistered("ActuatorService");
        actuatorAdapter = (ActuatorDataIface) getRegistered("actuatorAdapter");
        //widgetAdapter = (WidgetAdapterIface) getRegistered("widgetAdapter");
        scriptingAdapter = (ScriptingAdapterIface) getRegistered("scriptingAdapter");
        //notifications
        smtpNotification = (NotificationIface) getRegistered("smtpNotification");
        smsNotification = (NotificationIface) getRegistered("smsNotification");
        pushoverNotification = (NotificationIface) getRegistered("pushoverNotification");
        slackNotification = (NotificationIface) getRegistered("slackNotification");
        telegramNotification = (NotificationIface) getRegistered("telegramNotification");
        discordNotification = (NotificationIface) getRegistered("discordNotification");
        webhookNotification = (NotificationIface) getRegistered("webhookNotification");
        emailSender = (EmailSenderIface) getRegistered("emailSender");
        mailingAdapter = (MailingIface) getRegistered("MailingService");

        loraUplinkService = (LoRaApi) getRegistered("LoRaUplinkService");
        ttnIntegrationService = (TtnApi) getRegistered("TtnIntegrationService");
        kpnUplinkService = (KpnApi) getRegistered("KpnUplinkService");

        shortenerDB = (ShortenerDBIface) getRegistered("ShortenerDB");

        apiGenerator = (OpenApiIface) getRegistered("OpenApi");
    }

    public IotDatabaseIface getThingsAdapter() {
        return thingsDB;
    }

    @Override
    public void runInitTasks() {
        try {
            super.runInitTasks();
            EventMaster.registerEventCategories(new Event().getCategories(), Event.class.getName());
            EventMaster.registerEventCategories(new UserEvent().getCategories(), UserEvent.class.getName());
            EventMaster.registerEventCategories(new IotEvent().getCategories(), IotEvent.class.getName());
            EventMaster.registerEventCategories(new Alert().getCategories(), Alert.class.getName());
        } catch (EventException | InitException ex) {
            ex.printStackTrace();
            shutdown();
        }
        //read the OS variable to get the service URL
        String urlEnvName = (String) getProperties().get("SRVC_URL_ENV_VARIABLE");
        if (null != urlEnvName) {
            try {
                String url = System.getenv(urlEnvName);
                if (null != url) {
                    getProperties().put("serviceurl", url);
                }
            } catch (Exception e) {
            }
        }
        invariants = new Invariants();
        PlatformAdministrationModule.getInstance().initDatabases(
                database, userDB, authDB, thingsDB,
                iotDataDB, actuatorCommandsDB, shortenerDB);
        //PlatformAdministrationModule.getInstance().readPlatformConfig(database);
        //TODO: use services monitoring
        //PlatformAdministrationModule.getInstance().initScheduledTasks(scheduler);
        //TODO: na tym się potrafi zawiesić
        Kernel.getInstance().handleEvent(
                new Event(
                        this.getClass().getSimpleName(),
                        Event.CATEGORY_GENERIC,
                        "EMAIL_ADMIN_STARTUP",
                        "+1s",
                        "Signomix service has been started.")
        );
        apiGenerator.init(this);
        setInitialized(true);
    }

    @Override
    public void runFinalTasks() {
        /*
        // CLI adapter doesn't start automaticaly as other inbound adapters
        if (cli != null) {
            cli.start();
        }
         */
    }

    /**
     * Executed when the Service is started in "not service" mode
     */
    @Override
    public void runOnce() {
        super.runOnce();
        handleEvent(Event.logInfo("Service.runOnce()", "executed"));
    }

    @Override
    public void shutdown() {
        emailSender.send(
                (String) getProperties().getOrDefault("admin-notification-email", ""),
                "Signomix - shutdown", "Signomix service is going down."
        );
        super.shutdown();
    }

    /**
     * Event dispatcher method. Depending on the event category, Service and
     * QueueAdapte configurations dispatches event to Scheduler, QueAdapter or
     * Kernel handler method.
     *
     * @param event Event object to dispatch
     * @return
     */
    @Override
    public Object handleEvent(Event event) {
        if (queueAdapter != null && queueAdapter.isHandling(event.getCategory())) {
            try {
                queueAdapter.send(event);
            } catch (QueueException ex) {
                handleEvent(Event.logSevere(this.getClass().getSimpleName(), ex.getMessage()));
            }
            return null;
        }
        if (scheduler != null && event.getTimePoint() != null) {
            scheduler.handleEvent(event);
            return null;
        }
        return super.handleEvent(event);
    }

    @HttpAdapterHook(adapterName = "goto", requestMethod = "GET")
    public Object goToShortcut(Event event) {
        return UrlShortener.getInstance().processRequest(event, shortenerDB);
    }

    @HttpAdapterHook(adapterName = "goto", requestMethod = "POST")
    public Object updateShortcut(Event event) {
        return UrlShortener.getInstance().processRequest(event, shortenerDB);
    }

    /**
     * Process requests from simple web server implementation given by
     * HtmlGenAdapter access web web resources
     *
     * @param event
     * @return ParameterMapResult with the file content as a byte array
     */
    @HttpAdapterHook(adapterName = "WwwService", requestMethod = "GET")
    public Object wwwGet(Event event) {
        ArrayList<String> cookies = null;
        try {
            event.getRequest().headers.keySet().forEach(key -> {
                if (key.equalsIgnoreCase("Cookie")) {
                    event.getRequest().headers.get(key).forEach(value -> {
                        cookies.add(key);
                    });
                }

            });
            //cookies.forEach(value -> System.out.println("Cookie: " + value));
        } catch (Exception e) {
        }

        ParameterMapResult result = new ParameterMapResult();
        String userID = null;
        try {
            //TODO: to nie jest optymalne rozwiązanie
            Kernel.getInstance().dispatchEvent(Event.logWarning(this, "GET WWW: " + event.getRequest().pathExt));
            dispatchEvent(Event.logFinest(this.getClass().getSimpleName(), event.getRequest().uri));
            String language = event.getRequestParameter("language");
            if (language == null || language.isEmpty()) {
                language = "en";
            } else if (language.endsWith("/")) {
                language = language.substring(0, language.length() - 1);
            }

            try {
                String cacheName = "webcache_" + language;
                result = (ParameterMapResult) cms
                        .getFile(event.getRequest(), htmlAdapter.useCache() ? database : null, cacheName, language);

                if (ResponseCode.NOT_FOUND == result.getCode()) {
                    if (event.getRequest().pathExt.endsWith(".html")) {
                        //TODO: configurable index file params
                        //RequestObject request = processRequest(event.getRequest(), ".html", "index_pl.html");
                        RequestObject request = processRequest(event.getRequest(), ".html", "index.html");
                        result = (ParameterMapResult) fileReader
                                .getFile(request, htmlAdapter.useCache() ? database : null, "webcache_en");
                    }
                }

                if (ResponseCode.NOT_FOUND == result.getCode()) {
                    Kernel.getInstance().dispatchEvent(Event.logWarning(this, "404 WWW: " + event.getRequest().pathExt));
                    return result;
                } else if (ResponseCode.NOT_FOUND != result.getCode()
                        && ("".equals(event.getRequest().pathExt) || event.getRequest().pathExt.endsWith("/") || event.getRequest().pathExt.endsWith(".html"))) {
                    //((HashMap) result.getData()).put("serviceurl", getProperties().get("serviceurl"));
                    userID = event.getRequest().headers.getFirst("X-user-id");
                    HashMap rd = (HashMap) result.getData();
                    rd.put("serviceurl", getProperties().get("serviceurl"));
                    rd.put("defaultLanguage", getProperties().get("default-language"));
                    rd.put("gaTrackingID", getProperties().get("ga-tracking-id"));
                    rd.put("token", event.getRequestParameter("tid"));  // fake tokens doesn't pass SecurityFilter
                    rd.put("shared", event.getRequestParameter("tid"));  // niepusty tid może być permanentnym tokenem ale może też być fałszywy
                    rd.put("user", userID);
                    rd.put("environmentName", getName());
                    rd.put("distroType", (String) invariants.get("release"));
                    rd.put("javaversion", System.getProperty("java.version"));
                    List<String> roles = event.getRequest().headers.get("X-user-role");
                    if (roles != null) {
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < roles.size(); i++) {
                            if (i > 0) {
                                sb.append(",");
                            }
                            sb.append("'").append(roles.get(i)).append("'");
                        }
                        sb.append("]");
                        rd.put("roles", sb.toString());
                    } else {
                        rd.put("roles", "[]");
                    }
                    // TODO: caching policy 
                    result.setMaxAge(120);
                    if (null != userID && !userID.isEmpty()) {
                        result.setHeader("X-user-id", userID);
                    } else {
                        result.setHeader("X-user-id", "guest");
                    }
                    //if (null != cookies && cookies.get(0).indexOf(SIGNOMIX_TOKEN_NAME) >= 0) {
                    //    result.setHeader("Cookie", cookies.get(0));
                    //}
                }
            } catch (Exception e) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this, "500 WWW: " + event.getRequest().pathExt));
                e.printStackTrace();
                result = new ParameterMapResult();
                result.setCode(ResponseCode.INTERNAL_SERVER_ERROR);
                return result;
            }
            if ("HEAD".equalsIgnoreCase(event.getRequest().method)) {
                byte[] empty = {};
                result.setPayload(empty);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.setCode(ResponseCode.INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    /**
     * Modify request pathExt basic on adapter configuration for CMS/Website
     * systems
     *
     * @param originalRequest
     * @param indexFileExt
     * @param indexFileName
     * @return
     */
    private RequestObject processRequest(RequestObject originalRequest, String indexFileExt, String indexFileName) {
        RequestObject request = originalRequest;
        String[] pathElements = request.uri.split("/");
        if (pathElements.length == 0) {
            return request;
        }
        StringBuilder sb = new StringBuilder();
        if (pathElements[pathElements.length - 1].endsWith(indexFileExt)) {
            if (!pathElements[pathElements.length - 1].equals(indexFileName)) {
                for (int i = 0; i < pathElements.length - 1; i++) {
                    sb.append(pathElements[i]).append("/");
                }
                request.pathExt = sb.toString();
            }
        }
        return request;
    }

    @PortEventClassHook(className = "MailingApiEvent", procedureName = "send")
    public Object handleMailingSend(MailingApiEvent event) {
        return mailingAdapter.sendMailing(event.getData().get("documentId"), event.getData().get("target"), userAdapter, cms, emailSender);
    }

    @PortEventClassHook(className = "AlertApiEvent", procedureName = "get")
    public Object handleAlertGet(AlertApiEvent event) {
        return AlertModule.getInstance().getAlerts(event.userId, thingsAdapter);
    }

    @PortEventClassHook(className = "AlertApiEvent", procedureName = "delete")
    public Object handleAlertDelete(AlertApiEvent event) {
        if (!"*".equals(event.alertId)) {
            return AlertModule.getInstance().removeAlert(event.userId, event.alertId, thingsAdapter);
        } else {
            return AlertModule.getInstance().removeAll(event.userId, thingsAdapter);
        }
    }

    /*
    @HttpAdapterHook(adapterName = "AlertService", requestMethod = "OPTIONS")
    public Object alertCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }
    @HttpAdapterHook(adapterName = "AlertService", requestMethod = "GET")
    public Object alertGet(Event event) {
        return AlertModule.getInstance().getAlerts(event, thingsAdapter);
    }
    @HttpAdapterHook(adapterName = "AlertService", requestMethod = "DELETE")
    public Object alertDelete(Event event) {
        String alertId = event.getRequest().pathExt;
        if (alertId != null && !alertId.isEmpty()) {
            return AlertModule.getInstance().removeAlert(event, thingsAdapter);
        } else {
            return AlertModule.getInstance().removeAll(event.getRequestParameter("user"), thingsAdapter);
        }
    }
     */
    @HttpAdapterHook(adapterName = "DashboardService", requestMethod = "OPTIONS")
    public Object dashboardServiceOptions(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "DashboardService", requestMethod = "GET")
    public Object dashboardServiceGet(Event event) {
        try {
            return new DashboardBusinessLogic().getInstance().processEvent(event, dashboardAdapter, thingsAdapter, authAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @HttpAdapterHook(adapterName = "DashboardService", requestMethod = "POST")
    public Object dashboardServicePost(Event event) {
        try {
            return new DashboardBusinessLogic().getInstance().processEvent(event, dashboardAdapter, thingsAdapter, authAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @HttpAdapterHook(adapterName = "DashboardService", requestMethod = "PUT")
    public Object dashboardServicePut(Event event) {
        try {
            return new DashboardBusinessLogic().getInstance().processEvent(event, dashboardAdapter, thingsAdapter, authAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @HttpAdapterHook(adapterName = "DashboardService", requestMethod = "DELETE")
    public Object dashboardServiceDelete(Event event) {
        try {
            return new DashboardBusinessLogic().getInstance().processEvent(event, dashboardAdapter, thingsAdapter, authAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @HttpAdapterHook(adapterName = "DeviceService", requestMethod = "OPTIONS")
    public Object deviceServiceOptions(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "DeviceService", requestMethod = "GET")
    public Object deviceServiceGet(Event event) {
        StandardResult result = (StandardResult) new DeviceManagementModule().processDeviceEvent(event, thingsAdapter, userAdapter, PlatformAdministrationModule.getInstance());
        return result;
    }

    @HttpAdapterHook(adapterName = "DeviceService", requestMethod = "POST")
    public Object deviceServicePost(Event event) {
        StandardResult result = (StandardResult) new DeviceManagementModule().processDeviceEvent(event, thingsAdapter, userAdapter, PlatformAdministrationModule.getInstance());
        return result;
    }

    @HttpAdapterHook(adapterName = "DeviceService", requestMethod = "PUT")
    public Object deviceServicePut(Event event) {
        StandardResult result = (StandardResult) new DeviceManagementModule().processDeviceEvent(event, thingsAdapter, userAdapter, PlatformAdministrationModule.getInstance());
        return result;
    }

    @HttpAdapterHook(adapterName = "DeviceService", requestMethod = "DELETE")
    public Object deviceServiceDelete(Event event) {
        StandardResult result = (StandardResult) new DeviceManagementModule().processDeviceEvent(event, thingsAdapter, userAdapter, PlatformAdministrationModule.getInstance());
        return result;
    }

    @HttpAdapterHook(adapterName = "GroupService", requestMethod = "OPTIONS")
    public Object groupServiceOptions(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "GroupService", requestMethod = "GET")
    public Object groupServiceGet(Event event) {
        return new DeviceManagementModule().processGroupEvent(event, thingsAdapter, userAdapter, PlatformAdministrationModule.getInstance());
    }

    @HttpAdapterHook(adapterName = "GroupService", requestMethod = "POST")
    public Object groupServicePost(Event event) {
        return new DeviceManagementModule().processGroupEvent(event, thingsAdapter, userAdapter, PlatformAdministrationModule.getInstance());
    }

    @HttpAdapterHook(adapterName = "GroupService", requestMethod = "PUT")
    public Object groupServicePut(Event event) {
        return new DeviceManagementModule().processGroupEvent(event, thingsAdapter, userAdapter, PlatformAdministrationModule.getInstance());
    }

    @HttpAdapterHook(adapterName = "GroupService", requestMethod = "DELETE")
    public Object groupServiceDelete(Event event) {
        return new DeviceManagementModule().processGroupEvent(event, thingsAdapter, userAdapter, PlatformAdministrationModule.getInstance());
    }

    @HttpAdapterHook(adapterName = "TemplateService", requestMethod = "OPTIONS")
    public Object templateServiceCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "TemplateService", requestMethod = "*")
    public Object templateServiceHandle(Event event) {
        return new DeviceManagementModule().processTemplateEvent(event, thingsAdapter, userAdapter, PlatformAdministrationModule.getInstance());
    }

    @HttpAdapterHook(adapterName = "TtnIntegrationService", requestMethod = "OPTIONS")
    public Object ttnDataCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "TtnIntegrationService", requestMethod = "*")
    public Object ttnDataAdd(Event event) {
        try {
            return DeviceIntegrationModule.getInstance().processTtnRequest(event, thingsAdapter, userAdapter, scriptingAdapter, ttnIntegrationService);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PortEventClassHook(className = "UplinkEvent", procedureName = "processData")
    public Object handleChirpstackUplink(UplinkEvent requestEvent) {
        IotData data = (IotData) requestEvent.getOriginalEvent().getPayload();
        String info = "RECEIVED: application=%1$s, devEUI=%2$s, data=%3$s";
        try {
            return DeviceIntegrationModule.getInstance().processChirpstackRequest(data, thingsAdapter, userAdapter, scriptingAdapter, ttnIntegrationService);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    @PortEventClassHook(className = "NewDataEvent", procedureName = "processData")
    public Object handleIotData(NewDataEvent requestEvent) {
        IotData data = (IotData) requestEvent.getOriginalEvent().getPayload();
        try {
            return DeviceIntegrationModule.getInstance().processGenericRequest(data, thingsAdapter, userAdapter, scriptingAdapter, ttnIntegrationService, actuatorCommandsDB);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*

    @HttpAdapterHook(adapterName = "IntegrationService", requestMethod = "OPTIONS")
    public Object iotDataCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "IntegrationService", requestMethod = "*")
    public Object iotDataAdd(Event event) {
        StandardResult result;
        try {
            result = (StandardResult) DeviceIntegrationModule.getInstance().processIotRequest(event, thingsAdapter, userAdapter, scriptingAdapter, integrationService, actuatorCommandsDB);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    @HttpAdapterHook(adapterName = "SimpleIntegrationService", requestMethod = "OPTIONS")
    public Object iotSimpleDataCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "SimpleIntegrationService", requestMethod = "*")
    public Object iotSimpleDataAdd(Event event) {
        StandardResult result;
        try {
            result = (StandardResult) DeviceIntegrationModule.getInstance().processIotRequest(event, thingsAdapter, userAdapter, scriptingAdapter, integrationService, actuatorCommandsDB);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    @HttpAdapterHook(adapterName = "RawIntegrationService", requestMethod = "OPTIONS")
    public Object rawDataCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "RawIntegrationService", requestMethod = "*")
    public Object rawDataAdd(Event event) {
        StandardResult result;
        try {
            result = (StandardResult) DeviceIntegrationModule.getInstance().processRawRequest(event, thingsAdapter, userAdapter, scriptingAdapter, rawIntegrationService, actuatorCommandsDB);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

     */
    @HttpAdapterHook(adapterName = "ActuatorService", requestMethod = "OPTIONS")
    public Object actuatorCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "ActuatorService", requestMethod = "*")
    public Object actuatorHandle(Event event) {
        return ActuatorModule.getInstance().processRequest(event, actuatorApi, thingsAdapter, actuatorCommandsDB, scriptingAdapter);
    }

    @HttpAdapterHook(adapterName = "LoRaUplinkService", requestMethod = "*")
    public Object LoRaUplinkHandle(Event event) {
        return DeviceIntegrationModule.getInstance().processLoRaRequest(event, thingsAdapter, userAdapter, scriptingAdapter, loraUplinkService);
    }

    @HttpAdapterHook(adapterName = "LoRaJoinService", requestMethod = "*")
    public Object LoRaJoinHandle(Event event) {
        return LoRaBusinessLogic.getInstance().processLoRaRequest(event);
    }

    @HttpAdapterHook(adapterName = "LoRaAckServDeviceice", requestMethod = "*")
    public Object LoRaAckHandle(Event event) {
        return LoRaBusinessLogic.getInstance().processLoRaRequest(event);
    }

    @HttpAdapterHook(adapterName = "LoRaErrorService", requestMethod = "*")
    public Object LoRaErrorHandle(Event event) {
        return LoRaBusinessLogic.getInstance().processLoRaRequest(event);
    }

    @HttpAdapterHook(adapterName = "KpnUplinkService", requestMethod = "*")
    public Object KpnUplinkHandle(Event event) {
        return DeviceIntegrationModule.getInstance().processKpnRequest(event, thingsAdapter, userAdapter, scriptingAdapter, kpnUplinkService);
    }

    @HttpAdapterHook(adapterName = "RecoveryService", requestMethod = "OPTIONS")
    public Object recoveryCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "RecoveryService", requestMethod = "POST")
    public Object recoveryHandle(Event event) {
        String resetPassEmail = event.getRequestParameter("resetpass");
        String userName = event.getRequestParameter("name");
        return CustomerModule.getInstance().handleResetRequest(event, userName, resetPassEmail, userAdapter, authAdapter, emailSender);
    }

    @HttpAdapterHook(adapterName = "UserService", requestMethod = "OPTIONS")
    public Object userCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    /**
     * Return user data
     *
     * @param event
     * @return
     */
    @HttpAdapterHook(adapterName = "UserService", requestMethod = "GET")
    public Object userGet(Event event) {
        return UserModule.getInstance().handleGetRequest(event, userAdapter);
    }

    @HttpAdapterHook(adapterName = "UserService", requestMethod = "POST")
    public Object userAdd(Event event) {
        boolean withConfirmation = "true".equalsIgnoreCase((String) getProperties().getOrDefault("user-confirm", "false"));
        return UserModule.getInstance().handleRegisterRequest(event, userAdapter, withConfirmation, telegramNotification);
    }

    @HttpAdapterHook(adapterName = "SubscriberService", requestMethod = "POST")
    public Object subscriberAdd(Event event) {
        boolean withConfirmation = "true".equalsIgnoreCase((String) getProperties().getOrDefault("user-confirm", "false"));
        return UserModule.getInstance().handleSubscribeRequest(event, userAdapter, withConfirmation, telegramNotification);
    }

    /**
     * Modify user data or sends password reset link
     *
     * @param event
     * @return
     */
    @HttpAdapterHook(adapterName = "UserService", requestMethod = "PUT")
    public Object userUpdate(Event event) {
        String resetPassEmail = event.getRequestParameter("resetpass");
        try {
            if (resetPassEmail == null || resetPassEmail.isEmpty()) {
                return UserModule.getInstance().handleUpdateRequest(event, userAdapter, telegramNotification);
            } else {
                String userName = event.getRequestParameter("name");
                return CustomerModule.getInstance().handleResetRequest(event, userName, resetPassEmail, userAdapter, authAdapter, emailSender);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Set user as waiting for removal
     *
     * @param event
     * @return
     */
    @HttpAdapterHook(adapterName = "UserService", requestMethod = "DELETE")
    public Object userDelete(Event event) {
        boolean withConfirmation = "true".equalsIgnoreCase((String) getProperties().getOrDefault("user-confirm", "false"));
        return UserModule.getInstance().handleDeleteRequest(event, userAdapter, withConfirmation);
    }

    @HttpAdapterHook(adapterName = "AuthService", requestMethod = "OPTIONS")
    public Object authCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "AuthService", requestMethod = "POST")
    public Object authLogin(Event event) {
        System.out.println("LOGIN");
        StandardResult result = (StandardResult) AuthBusinessLogic.getInstance().login(event, authAdapter);
        if (result.getCode() == ResponseCode.OK) {
            Kernel.getInstance().dispatchEvent(new SystemEvent(SystemEvent.USER, "login"));
        } else {
            Kernel.getInstance().dispatchEvent(new SystemEvent(SystemEvent.USER, "login_error"));
        }
        return result;
    }

    @HttpAdapterHook(adapterName = "AuthService", requestMethod = "DELETE")
    public Object authLogout(Event event) {
        return AuthBusinessLogic.getInstance().logout(event, authAdapter);
    }

    @HttpAdapterHook(adapterName = "AuthService", requestMethod = "GET")
    public Object authCheck(Event event) {
        return AuthBusinessLogic.getInstance().check(event, authAdapter);
    }

    @HttpAdapterHook(adapterName = "AuthService", requestMethod = "PUT")
    public Object authRefresh(Event event) {
        return AuthBusinessLogic.getInstance().refreshToken(event, authAdapter);
    }

    @HttpAdapterHook(adapterName = "ConfirmationService", requestMethod = "GET")
    public Object userConfirm(Event event) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.FORBIDDEN);
        try {
            String key = event.getRequestParameter("key");
            try {
                if (authAdapter.checkToken(key)) {
                    User user = authAdapter.getUser(key);
                    if (user.getStatus() == User.IS_REGISTERING && user.getConfirmString().equals(key)) {
                        user.setConfirmed(true);
                        userAdapter.modify(user);
                        result.setCode(200);
                        //TODO: build default html page or redirect
                        String pageContent
                                = "Registration confirmed.<br>You can go to <a href=/#!login>login page</a> and sign in.";
                        result.setFileExtension("html");
                        result.setHeader("Content-type", "text/html");
                        //result.setData(pageContent);
                        result.setPayload(pageContent.getBytes());
                    }
                } else {
                    result.setCode(401);
                    String pageContent
                            = "Oops, something has gone wrong: confirmation token not found . We cannot confirm your <a href=/>Signomix</a> registration. Please contact support.";
                    result.setFileExtension("html");
                    result.setHeader("Content-type", "text/html");
                    result.setPayload(pageContent.getBytes());
                }
            } catch (UserException ex) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "confirmation error " + ex.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @HttpAdapterHook(adapterName = "SubscriptionConfirmationService", requestMethod = "GET")
    public Object subscriptionConfirm(Event event) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.FORBIDDEN);
        try {
            String key = event.getRequestParameter("key");
            try {
                if (authAdapter.checkToken(key)) {
                    User user = authAdapter.getUser(key);
                    if (user.getStatus() == User.IS_REGISTERING && user.getConfirmString().equals(key)) {
                        user.setConfirmed(true);
                        userAdapter.modify(user);
                        result.setCode(200);
                        //TODO: build default html page or redirect
                        String pageContent
                                = "Subscription confirmed.<br>Thank you for using <a href='/'>Signomix</a>.";
                        result.setFileExtension("html");
                        result.setHeader("Content-type", "text/html");
                        //result.setData(pageContent);
                        result.setPayload(pageContent.getBytes());
                    }
                } else {
                    result.setCode(401);
                    String pageContent
                            = "Oops, something has gone wrong: confirmation token not found . Your subscription cannot be confirmed.";
                    result.setFileExtension("html");
                    result.setHeader("Content-type", "text/html");
                    result.setPayload(pageContent.getBytes());
                }
            } catch (UserException ex) {
                Kernel.getInstance().dispatchEvent(Event.logWarning(this.getClass().getSimpleName(), "confirmation error " + ex.getMessage()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @HttpAdapterHook(adapterName = "ContentService", requestMethod = "OPTIONS")
    public Object contentCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "ContentService", requestMethod = "GET")
    public Object contentGetPublished(Event event) {
        StandardResult result = null;
        try {
            result = (StandardResult) new ContentRequestProcessor().processGetPublished(event, cms);
            if (ResponseCode.NOT_FOUND == result.getCode()) {
                String path = event.getRequest().pathExt;
                if (null != path) {
                    String filePath = fileReader.getRootPath() + path;
                    Document doc = new Document();
                    doc.setTitle("");
                    doc.setSummary("");
                    doc.setAuthor("");
                    doc.setContent(new String(fileReader.readFile(filePath)));
                    result.setData(doc);
                    result.setCode(ResponseCode.OK);
                }
                Kernel.getInstance().dispatchEvent(Event.logWarning(this, "missing in CMS so read form disk " + path));
            }
        } catch (Exception e) {
            result = new StandardResult();
            result.setCode(ResponseCode.NOT_FOUND);
        }
        return result;
    }

    @HttpAdapterHook(adapterName = "ContentManager", requestMethod = "OPTIONS")
    public Object contentServiceCors(Event requestEvent) {
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.OK);
        return result;
    }

    @HttpAdapterHook(adapterName = "ContentManager", requestMethod = "*")
    public Object contentServiceHandle(Event event) {
        return new ContentRequestProcessor().processRequest(event, cms, null);
    }

    @HttpAdapterHook(adapterName = "SystemService", requestMethod = "*")
    public Object systemServiceHandle(Event event) {
        return new PlatformAdministrationModule().handleRestEvent(event);
    }

    @EventHook(eventCategory = Event.CATEGORY_LOG)
    public void logEvent(Event event) {
        try {
            logAdapter.log(event);
            if (event.getType().equals(Event.LOG_SEVERE)) {
                emailSender.send((String) getProperties().getOrDefault("admin-notification-email", ""), "Signomix - error", event.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHook(eventCategory = Event.CATEGORY_HTTP_LOG)
    public void logHttpEvent(Event event) {
        try {
            logAdapter.log(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHook(eventCategory = UserEvent.CATEGORY_USER)
    public void processUserEvent(Event event) {
        try {
            if (event.getTimePoint() != null) {
                scheduler.handleEvent(event);
                return;
            }
            UserEventHandler.handleEvent(
                    this,
                    event,
                    userAdapter,
                    gdprLogger,
                    authAdapter,
                    thingsAdapter,
                    dashboardAdapter,
                    emailSender
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHook(eventCategory = SystemEvent.CATEGORY_SYSTEM)
    public void processPlatformEvent(Event event) {
        if (event.getTimePoint() != null) {
            scheduler.handleEvent(event);
        } else {
            if (event.getType().equals(SystemEvent.MONITORING)) {
                String eui = (String) getProperties().getOrDefault("monitoring_device", "");
                if (!eui.isEmpty()) {
                    Device d = null;
                    try {
                        d = thingsAdapter.getDevice(eui);
                    } catch (ThingsDataException ex) {
                    }
                    if (null != d) {
                        event.setOrigin("@" + eui); //source@target
                        ActuatorModule.getInstance().processCommand(event, false, actuatorCommandsDB, thingsAdapter, scriptingAdapter);
                    }
                }
            }
        }
    }

    @EventHook(eventCategory = IotEvent.CATEGORY_IOT)
    public void processIotEvent(Event event) {
        try {
            IotEventHandler.handleEvent(
                    this,
                    event,
                    scheduler,
                    userAdapter,
                    thingsAdapter,
                    smtpNotification,
                    smsNotification,
                    pushoverNotification,
                    slackNotification,
                    telegramNotification,
                    discordNotification,
                    webhookNotification,
                    dashboardAdapter,
                    authAdapter,
                    scriptingAdapter,
                    actuatorCommandsDB
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles system events
     *
     * @param event event object to process
     */
    @EventHook(eventCategory = Event.CATEGORY_GENERIC)
    public void processSystemEvent(Event event) {
        try {
            SystemEventHandler.handleEvent(
                    this,
                    event,
                    database,
                    cmsDatabase,
                    userAdapter,
                    userDB,
                    authAdapter,
                    authDB,
                    actuatorAdapter,
                    actuatorCommandsDB,
                    thingsAdapter,
                    thingsDB,
                    iotDataDB,
                    dashboardAdapter,
                    scriptingAdapter,
                    emailSender
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles all event categories not processed by other handler methods
     *
     * @param event event object to process
     */
    @EventHook(eventCategory = "*")
    public void processEvent(Event event) {
        try {
            if (event.getTimePoint() != null) {
                scheduler.handleEvent(event);
            } else {
                handleEvent(Event.logWarning("Don't know how to handle category " + event.getCategory(), event.getPayload().toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
