/**
 * Copyright (C) Grzegorz Skorupa 2020.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.signomix.in.http;

import com.cedarsoftware.util.io.JsonIoException;
import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;
import com.signomix.event.UplinkEvent;
import com.signomix.iot.IotData;
import com.signomix.iot.chirpstack.uplink.Location;
import com.signomix.iot.chirpstack.uplink.RxInfo;
import com.signomix.iot.chirpstack.uplink.TxInfo;
import com.signomix.iot.chirpstack.uplink.Uplink;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.cricketmsf.Event;
import org.cricketmsf.Kernel;
import org.cricketmsf.RequestObject;
import org.cricketmsf.event.ProcedureCall;
import org.cricketmsf.in.http.HttpAdapter;
import org.cricketmsf.in.http.HttpPortedAdapter;

public class ChirpstackUplinkApi extends HttpPortedAdapter {

    private final int JSON3 = 0;
    private final int JSONPBUF = 2;
    private final int PBUF = 3;

    private int serializationType = JSON3;
    private boolean authorizationRequired = false;

    public ChirpstackUplinkApi() {
        super();
    }

    @Override
    public void loadProperties(HashMap<String, String> properties, String adapterName) {
        super.loadProperties(properties, adapterName);
        setContext(properties.get("context"));
        String sType = (String) properties.getOrDefault("serialization-type", "JSONv3");
        switch (sType.toUpperCase()) {
            case "JSONV3":
                serializationType = JSON3;
                Kernel.getLogger().printIndented("serialization-type=JSONv3");
                break;
            case "PROTPBUFJSON":
                serializationType = JSONPBUF;
                Kernel.getLogger().printIndented("serialization-type=ProtobufJSON");
                break;
            case "PROTOBUF":
                serializationType = PBUF;
                Kernel.getLogger().printIndented("serialization-type=Protobuf");
                break;
            default:
                serializationType = JSON3;
                Kernel.getLogger().printIndented("serialization-type=JSONv3 (default)");
                break;
        }
    }

    /**
     * Transforming request data to Event type required by the service adapter.
     *
     * @param request
     * @param rootEventId
     * @return Wrapper object for the event and the port procedure name.
     */
    protected ProcedureCall preprocess(RequestObject request, long rootEventId) {
        // validation and translation 
        String method = request.method;
        if ("POST".equalsIgnoreCase(method)) {
            return preprocessPost(request, rootEventId);
        } else if ("OPTIONS".equalsIgnoreCase(method)) {
            return ProcedureCall.respond(200, "OK");
        } else {
            return ProcedureCall.respond(HttpAdapter.SC_METHOD_NOT_ALLOWED, "error");
        }
    }

    private ProcedureCall preprocessPost(RequestObject request, long rootEventId) {
        String errorMessage = "";
        ProcedureCall result = null;
        Uplink uplink = null;
        String jsonString = request.body;
        try {
            StringBuilder sb = new StringBuilder(jsonString);
            System.out.println("RECEIVED:");
            System.out.println(sb.toString());
            try {
                HashMap opts = new HashMap();
                opts.put(JsonReader.USE_MAPS, true);
                switch (serializationType) {
                    case JSON3:
                        uplink = processObject((JsonObject) JsonReader.jsonToJava(sb.toString(), opts));
                        break;
                    default:
                        return ProcedureCall.respond(HttpAdapter.SC_NOT_IMPLEMENTED, "not supported serialization type");
                }
            } catch (JsonIoException e) {
                e.printStackTrace();
                errorMessage = e.getMessage();
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
            errorMessage = e.getMessage();
        } catch (NullPointerException e) {
            e.printStackTrace();
            errorMessage = e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = e.getMessage();
        }
        if (errorMessage.isEmpty()) {
            Event ev = new Event(this.getName(), request);
            ev.setRootEventId(rootEventId);
            ev.setPayload(
                    new IotData(uplink)
                            .authRequired(authorizationRequired)
                            .authKey(request.headers.getFirst("Authorization"))
                            .serializedData(jsonString)
            );
            UplinkEvent event = new UplinkEvent(ev);
            result = ProcedureCall.forward(event, "processData");
        } else {
            result = ProcedureCall.respond(-1, errorMessage);
        }
        return result;
    }

    private Uplink processObject(JsonObject o) {
        Uplink uplink = new Uplink();
        uplink.setAdr((boolean) o.get("adr"));
        uplink.setApplicationID((String) o.get("applicationID"));
        uplink.setApplicationName((String) o.get("applicationName"));
        uplink.setData((String) o.get("data"));
        uplink.setDevEUI((String) o.get("devEUI"));
        uplink.setDeviceName((String) o.get("deviceName"));
        uplink.setfCnt((long) o.get("fCnt"));
        uplink.setfPort((long) o.get("fPort"));

        //tx
        JsonObject txObj = (JsonObject) o.get("txInfo");
        TxInfo txInfo = new TxInfo();
        txInfo.setDr((long) txObj.get("dr"));
        txInfo.setFrequency((long) txObj.get("frequency"));
        uplink.setTxInfo(txInfo);

        //rx
        ArrayList<RxInfo> rxList = new ArrayList<>();
        //List<JsonObject> objList = (List) o.get("rxInfo");
        Object[] jo = (Object[]) o.get("rxInfo");
        JsonObject rxObj, locObj;
        RxInfo rxInfo;
        Location loc;

        for (int i = 0; i < jo.length; i++) {
            rxObj = (JsonObject) jo[i];
            rxInfo = new RxInfo();
            rxInfo.setGatewayID((String) rxObj.get("gatewayID"));
            rxInfo.setUplinkID((String) rxObj.get("uplinkID"));
            rxInfo.setName((String) rxObj.get("name"));
            rxInfo.setRssi((long) rxObj.get("rssi"));
            rxInfo.setLoRaSNR((long) rxObj.get("loRaSNR"));
            locObj = (JsonObject) rxObj.get("location");
            loc = new Location();
            loc.setLatitude((Double) locObj.get("latitude"));
            loc.setLongitude((Double) locObj.get("longitude"));
            loc.setAltitude((long) locObj.get("altitude"));
            rxInfo.setLocation(loc);
            rxList.add(rxInfo);
        }
        uplink.setRxInfo(rxList);

        //data
        JsonObject data = (JsonObject) o.get("object");
        JsonObject dataMap;
        JsonObject dataFieldsMap;
        Iterator it = data.keySet().iterator();
        Iterator it2, it3;
        String dataName;
        String dataIndex, dataField;
        Object dataValue;
        while (it.hasNext()) {
            dataName = (String) it.next();
            dataMap = (JsonObject) data.get(dataName);
            it2 = dataMap.keySet().iterator();
            while (it2.hasNext()) {
                dataIndex = (String) it2.next();
                dataValue = dataMap.get(dataIndex);
                if (dataValue instanceof Double) {
                    uplink.addField(dataName + "_" + dataIndex, (Double) dataValue);
                } else {
                    dataFieldsMap = (JsonObject) dataValue;
                    it3 = dataFieldsMap.keySet().iterator();
                    while (it3.hasNext()) {
                        dataField = (String) it3.next();
                        dataValue = dataFieldsMap.get(dataField);
                        uplink.addField(dataName + "_" + dataIndex + "_" + dataField, (Double) dataValue);
                    }
                }
            }
        }
        return uplink;
    }

}
