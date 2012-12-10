import webapp2, json

from google.appengine.ext import db
import base64
import os
from lastapikeys import API_KEY, API_SECRET
from lib import pylast

class User(db.Model):
    name = db.StringProperty()
    token = db.StringProperty()
    session = db.StringProperty()
    identifier = db.StringProperty()
    active = db.BooleanProperty()

class AuthPage(webapp2.RequestHandler):
    def get(self):
        if 'identifier' in self.request.GET:
            identifier = self.request.GET['identifier']
            user = User.all().filter("identifier = ", identifier).get()
            if user == None:
                # error
                pass
            else:
                if user.session == None:
                    network = pylast.LastFMNetwork(api_key=API_KEY,api_secret=API_SECRET)
                    keyGen = pylast.SessionKeyGenerator(network)
                    if 'token' not in self.request.GET:
                        # redirect to api auth with callback here
                        callback = 'http://' + self.request.headers['Host'] + '/api/auth/?identifier=' + identifier 
                        self.redirect(keyGen.get_web_auth_url(callback))
                    else:
                        # get session
                        # show a webpage telling to close the browser
                        token = self.request.GET['token']
                        result = keyGen.get_web_auth_session_key(token)
                        user.name = result['name']
                        user.session = result['key']
                        user.put()
                        
                        self.response.headers['Content-Type'] = 'text/html'
                        self.response.write('<html><body>Well done ' + user.name + '. You can now go back to the app.</body></html>')
                else:
                    # return username
                    user.active = True
                    user.put()
                    
                    self.response.headers['Content-Type'] = 'application/json'
                    self.response.write(json.dumps({"name": user.name}))
        else:
            # create new user
            identifier =  base64.urlsafe_b64encode(os.urandom(30))
            User(active=False, identifier=identifier).put()
            
            self.response.headers['Content-Type'] = 'application/json'
            self.response.write(json.dumps({"identifier": identifier}))
        

app = webapp2.WSGIApplication([('/api/auth/', AuthPage)],
                              debug=True)
                              
