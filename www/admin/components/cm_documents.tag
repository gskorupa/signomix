<cm_documents>
    <div class="row" if={ selected }>
         <div class="col-md-12">
            <cm_document_form ref="doc_edit"></cm_document_form>
        </div>
    </div>
    <div class="row" if={ !selected }>
        <div class="col-md-12">
            <h2>{labels.title[app.language]} 
                <virtual each={ lang, i in app.languages}>
                    <button type="button" class="btn btn-sm { lang==selectedLanguage?'btn-primary':'btn-secondary' }" onclick={ selectLanguage(lang) }>{ lang }</button>
                </virtual>
                <i class="fa fa-refresh" aria-hidden="true" onclick={ refreshDocs() }>&nbsp;</i>
                <i class="fa fa-plus" aria-hidden="true" onclick={ editDocument('NEW', true) }>&nbsp;</i>
            </h2>
            <form class="form-inline">
                <div class="form-group">
                    <label for="pathDropdown">{ labels.path_status[app.language] } </label>
                    <select id="pathsDropdown" onchange={ selectPath } class="form-control">
                        <option each={ tmpPath, index in paths }>{ tmpPath }</option>
                    </select>
                    <select id="statusesDropdown" onchange={ selectStatus } class="form-control">
                        <option each={ tmpStatus, index in statuses }>{ tmpStatus }</option>
                    </select>
                </div>
            </form>
            <table id="doclist" class="table table-condensed">
                <thead>
                    <tr>
                        <th>{labels.t_name[app.language]}</th>
                        <th>{labels.t_title[app.language]}</th>
                        <th>{labels.t_status[app.language]}</th>
                        <th class="text-right">
                            <i class="fa fa-plus" aria-hidden="true" onclick={ editDocument('NEW', true) }></i>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    <tr each={doc in documents}>
                        <td>{ doc.name }</td>
                        <td>{ decodeURIComponent(doc.title) }</td>
                        <td>{ doc.status }</td>
                        <td class="text-right">                              
                            <i class="fa fa-eye separated" aria-hidden="true" onclick={ editDocument(doc.uid, false) }>&nbsp;</i>
                            <i class="fa fa-pencil-square-o separated" aria-hidden="true" onclick={ editDocument(doc.uid, true) }>&nbsp;</i>
                            <i class="fa fa-play separated" aria-hidden="true" if={ doc.status=='wip'} onclick={ setPublished(doc.uid, true) }>&nbsp;</i>
                            <i class="fa fa-stop separated" aria-hidden="true" if={ doc.status=='published'} onclick={ setPublished(doc.uid, false) }>&nbsp;</i>
                            <i class="fa fa-trash-o separated" onclick={ select(doc.uid) } aria-hidden="true" data-toggle="modal" data-target="#removeDialog">&nbsp;</i>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
    <div class="row" >
        <div class="col-md-12">
            <div id="removeDialog" class="modal fade">
                <div class="modal-dialog modal-sm">
                    <div class="modal-content">
                        <div class="modal-header">
                            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                            <h4 class="modal-title">{labels.remove_title[app.language]}</h4>
                        </div>
                        <div class="modal-body">
                            <p>{labels.remove_question[app.language]}</p>
                            <p class="text-warning"><small>{labels.remove_info[app.language]}</small></p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal" onclick={ select('') }>{labels.cancel[app.language]}</button>
                            <button type="button" class="btn btn-primary" data-dismiss="modal" onclick={ removeDocument() }>{labels.remove[app.language]}</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <script charset="UTF-8">
        var self = this;
        self.listener = riot.observable();
        self.path = '/'
        self.status = 'wip'
        self.paths = ['/']
        self.statuses = ['wip', 'published']
        self.documents = []
        self.selected = ''
        self.selectedLanguage = 'EN'
        self.removing = ''

        //globalEvents.on('pageselected:documents', function (eventName) {
        this.on('mount', function(){
            self.selected = ''
            self.selectedLanguage = app.language
            console.log('PAGE DOCUMENTS')
            console.log(self.statuses)
            console.log(self.paths)
            readPaths()
        });
        
        self.listener.on('*', function (eventName) {
            app.log('LISTENER: ' + eventName)
            if(eventName.startsWith('submitted:')){
                self.selected = ''
                var currentPath=eventName.substring(10)
                console.log('CURRENT PATH: '+currentPath)
                if(currentPath.length>0){
                    self.path = currentPath
                }else{
                    self.path = '/'
                }
                riot.update()
                readPaths()
            }else if(eventName.startsWith('cancelled')){
                self.selected = ''
            }else{
                app.log('DOCUMENTS: ' + eventName)
            }
            /*switch (eventName){
                case 'submitted':
                self.selected = ''
                readPaths()
                //readMyDevices()  //this line results in logout,login error
                break
                case 'cancelled':
                self.selected = ''
                break
                default:
                app.log('DOCUMENTS: ' + eventName)
            }
            */
            riot.update()
        });
        
        var readPaths = function () {
            app.log('reading paths ...')
            getData(app.cmAPI + '?pathsonly=true', // url
                        null, // query
                        app.user.token, // token
                        updatePaths, // callback
                        self.listener, // event listener
                        'OK', // success event name
                        null, // error event name
                        app.debug, // debug switch
                        globalEvents         // application event listener
            );
        }

        var readContentList = function () {
            app.log('reading docs ...')
            getData(app.cmAPI + '?path=' + self.path + '&language=' + self.selectedLanguage + '&status=' + self.status, // url
                        null, // query
                        app.user.token, // token
                        updateList, // callback
                        self.listener, // event listener
                        'OK', // success event name
                        null, // error event name
                        app.debug, // debug switch
                        globalEvents         // application event listener
            );
        }

        var updatePaths = function (text) {
            app.log("PATHS: " + text)
            self.paths = JSON.parse(text)
            riot.update()
            if (self.paths.length > 0){
                //self.path = self.paths[0]
                var index = self.paths.indexOf(self.path)
                console.log('PATH INDEX:'+index)
                document.getElementById("pathsDropdown").selectedIndex = index
            } else{
                //self.path = '/'
                document.getElementById("pathsDropdown").selectedIndex = - 1
            }
            readContentList()
            riot.update()
            
        }

        var updateList = function (text) {
            app.log("DOCUMENTS: " + text)
            self.documents = JSON.parse(text)
            riot.update()
            var index = self.statuses.indexOf(self.status)
            document.getElementById("statusesDropdown").selectedIndex = index
            riot.update()
        }

        editDocument(docId, allowEdit){
            return function(e){
                e.preventDefault()
                self.selected = docId
                riot.update()
                app.log('SELECTED FOR EDITING: ' + docId)
                self.refs.doc_edit.init(self.listener, docId, allowEdit, self.selectedLanguage, self.status, self.path)
            }
        }

        setPublished(docId, isPublished){
            return function(e){
                var formData = {
                    'language': self.selectedLanguage,
                    'status': isPublished?'published':'wip'
                }
                sendData(
                formData,
                'PUT',
                app.cmAPI + docId,
                app.user.token,
                self.afterPublish, //TODO
                null,
                'submit:OK',
                'submit:ERROR',
                app.debug,
                null
                )
                app.log('SET PUBLISHED ' + isPublished)
                riot.update()
            }
        }

        refreshDocs(){
            return function(e){
                app.log('refreshing...')
                readContentList()
            }
        }

        self.afterPublish = function(object){
            var text = '' + object
            console.log('CALBACK: ' + object)
            if (text.startsWith('{')){
                readContentList()
            } else if (text.startsWith('error')){
                //alert(text)
            } else if (text.startsWith('[object MouseEvent')){
                //self.callbackListener.trigger('cancelled')
            }   
        }

        selectLanguage(newLanguage){
            return function(e){
                e.preventDefault()
                self.selectedLanguage = newLanguage
                readContentList()
            }
        }

        self.selectPath = function(e){
            var index = e.target.selectedIndex
            app.log(e.target.options[index].value)
            self.path = e.target.options[index].value
            readContentList()
        }
        
        self.selectStatus = function(e){
            var index = e.target.selectedIndex
            app.log(e.target.options[index].value)
            self.status = e.target.options[index].value
            readContentList()
        }
        
        select(uid){
            return function(e){
                e.preventDefault()
                self.removing = uid
                riot.update()
                console.log('DEL SELECTED ' + uid)
            }
        }
        
        removeDocument(){
            return function(e){
                e.preventDefault()
                console.log('REMOVING ' + self.removing + ' ...')
                deleteData(
                    app.cmAPI + self.removing,
                    app.user.token,
                    self.closeRemove,
                    null, //self.listener, 
                    'submit:OK',
                    'submit:ERROR',
                    app.debug,
                    null //globalEvents
                )
            }
        }
        
        self.closeRemove = function(object){
            var text = '' + object
            console.log('CALBACK: ' + object)
            if (text.startsWith('{')){
            //
            } else if (text.startsWith('error:')){
            //it should'n happen
            //alert(text)
            }
            self.removing = ''
            readContentList()
        }

        self.labels = {
        "t_name": {
        "en": "NAME",
                "pl": "NAZWA"
        },
                "t_title": {
                "en": "TITLE",
                        "pl": "TYTUŁ"
                },
                "t_status": {
                "en": "STATUS",
                        "pl": "STATUS"
                },
                "path_status": {
                "en": "Path / Status",
                        "pl": "Ścieżka / Status"
                },
                "title": {
                "en": "documents",
                        "pl": "dokumenty"
                },
                "remove": {
                "en": "Remove",
                        "pl": "Usuń"
                },
                "cancel": {
                "en": "Cancel",
                        "pl": "Porzuć"
                },
                "remove_question": {
                "en": "Do you want to remove selected document?",
                        "pl": "Czy chcesz usunąć wybrany dokument?"
                },
                "remove_info": {
                "en": "All language versions will be removed.",
                        "pl": "Zostaną usunięte wszystkie wersje językowe."
                },
                "remove_title": {
                "en": "Removing document",
                        "pl": "Usuwanie dokumentu"
                }
        }
    </script>
</cm_documents>