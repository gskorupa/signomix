<app_login>
    <div class="panel panel-default form-login">
        <div class="panel-body">
            <form onsubmit={ submitLoginForm }>
                <!--<h3 class="logo">{ app.texts.app_login.l_title[app.language] }</h3>-->
                <div class="form-group">
                    <label class="visuallyhidden" for="login">{ app.texts.app_login.l_name[app.language] }</label>
                    <input class="form-control" id="login" name="login" type="text" 
                               placeholder={ app.texts.app_login.l_name[app.language] } required>
                </div>
                <div class="form-group">
                    <label class="visuallyhidden" for="description">{ app.texts.app_login.l_password[app.language] }</label>
                    <input class="form-control" id="password" name="password" type="password" 
                               placeholder={ app.texts.app_login.l_password[app.language] } required>
                </div>
                <button type="submit" class="btn btn-block btn-primary">{ app.texts.app_login.l_save[app.language] }</button>
            </form>
        </div>
    </div>
    <script>
        self=this
        
        globalEvents.on('auth.error', function (event) {
            app.log("Login error!")
        });
        
        globalEvents.on('auth:loggedin', function (event) {
            app.log("Login success!")
            app.currentPage = 'main'
            getData(app.userAPI+'/'+app.user.name, null, app.user.token, saveUserData, globalEvents, 'user:ok', 'user.error', app.debug)
        });
        
        saveUserData = function(text){
            tmpUser = JSON.parse(text);
            app.user.role = tmpUser.role
            riot.update()
        }

        submitLoginForm = function(e){
            e.preventDefault()
            app.log("submitting ..."+e.target)
            loginSubmit(e.target, globalEvents, 'auth:loggedin', 'auth.error', app.debug);
            e.target.reset()
        }

        this.labels = {
            "l_title": {
                "en": "Signomix",
                "pl": "Signomix"
            },"l_name": {
                "en": "Name",
                "pl": "Nazwa"
            },
            "l_password": {
                "en": "Pasword",
                "pl": "Hasło"
            },
            "l_save": {
                "en": "Sign In",
                "pl": "Zaloguj się"
            }
        }
        
    </script>
    <style>
        .form-login{
            max-width: 300px;
            margin: 0 auto;
            text-align: center;
        }
        .logo{
            margin-bottom: 30px;
        }
        .visuallyhidden{
            border: 0;
            clip: rect(0,0,0,0);
            height: 1px;
            margin: -1px;
            overflow: hidden;
            padding: 0;
            position: absolute;
            width: 1px;
        }
    </style>
</app_login>
