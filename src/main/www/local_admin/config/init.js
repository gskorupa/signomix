/* 
 * Copyright 2017 Grzegorz Skorupa <g.skorupa at gmail.com>.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
var setLocalConfig = function(){
    app.config.brand = "SIGNOMIX"
    app.config.copyright = "2018 Grzegorz Skorupa"
    app.user.dashboardID = ''
    app.user.dashboards = []
    app.languages = ["en", "pl", "fr", "it"]
    app.localUid = 0
    app.dconf = {"widgets":[]} // configurations of user's widgets on the dashboard page
    app.widgets = [ // widgets on the dashboard page - hardcoded structure
        [{}, {}, {}, {}],
        [{}, {}, {}, {}],
        [{}, {}, {}, {}]
    ]
    
    document.title = 'SIGNOMIX'
}
