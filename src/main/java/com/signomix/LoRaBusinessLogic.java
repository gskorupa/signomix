/**
* Copyright (C) Grzegorz Skorupa 2018.
* Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
*/
package com.signomix;

import org.cricketmsf.Event;
import org.cricketmsf.RequestObject;
import org.cricketmsf.in.http.HttpPortedAdapter;
import org.cricketmsf.in.http.ResponseCode;
import org.cricketmsf.in.http.StandardResult;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class LoRaBusinessLogic {
    
    private static LoRaBusinessLogic logic;

    public static LoRaBusinessLogic getInstance() {
        if (logic == null) {
            logic = new LoRaBusinessLogic();
        }
        return logic;
    }
    
    public Object processLoRaRequest(Event event){
        RequestObject request = event.getRequest();
        StandardResult result = new StandardResult();
        result.setCode(ResponseCode.CREATED);
        result.setData("OK");
        System.out.println(HttpPortedAdapter.dumpRequest(request));
        return result;
    }
    
}
