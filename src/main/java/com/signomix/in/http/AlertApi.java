/**
 * Copyright (C) Grzegorz Skorupa 2018.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.in.http;

import org.cricketmsf.Adapter;
import java.util.HashMap;
import org.cricketmsf.Kernel;
import org.cricketmsf.in.http.HttpAdapter;
import org.cricketmsf.in.http.HttpAdapterIface;


/**
 * REST API for messaging system
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class AlertApi extends HttpAdapter implements HttpAdapterIface, Adapter {

    /**
     * This method is executed while adapter is instantiated during the service
     * start. It's used to configure the adapter according to the configuration.
     *
     * @param properties map of properties readed from the configuration file
     * @param adapterName name of the adapter set in the configuration file (can
     * be different from the interface and class name.
     */
    @Override
    public void loadProperties(HashMap<String, String> properties, String adapterName) {
        super.loadProperties(properties, adapterName);
        super.getServiceHooks(adapterName);
        setContext(properties.get("context"));
        Kernel.getInstance().getLogger().print("\tcontext=" + getContext());
        setExtendedResponse(properties.getOrDefault("extended-response", "false"));
        Kernel.getInstance().getLogger().print("\textended-response=" + isExtendedResponse());
        setDateFormat(properties.get("date-format"));
        Kernel.getInstance().getLogger().print("\tdate-format: " + getDateFormat());
    }

}
