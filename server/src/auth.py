import webapp2, json

from google.appengine.ext import deferred

from lib import pylast
from src import friends_import
from src.lastapikeys import API_KEY, API_SECRET
from src.model import Knurek


class AuthPage(webapp2.RequestHandler):
    def create_new_user(self):
        key = Knurek(active=False).put()
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(json.dumps({"identifier": key.id()}))
        

    def handle_authentication(self, user, identifier):
        network = pylast.LastFMNetwork(api_key=API_KEY,api_secret=API_SECRET)
        keyGen = pylast.SessionKeyGenerator(network)
        if 'token' not in self.request.GET:
            # redirect to api auth with callback here
            callback = 'http://{0}/api/auth/?identifier={1}'.format(self.request.headers['Host'], identifier) 
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
            
            deferred.defer(friends_import.fetch_from_lastfm, identifier)
    
    
    def get_existing_user(self, user):
        user.active = True
        user.put()
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(json.dumps({"name": user.name}))
    
    
    def handle_not_found(self, identifier):
        self.response.write('No such user')
    
    
    def get(self):
        if 'identifier' in self.request.GET:
            identifier = int(self.request.GET['identifier'])
            user = Knurek.get_by_id(identifier)
            if user == None:
                self.handle_not_found(identifier)
            else:
                if user.session == None:
                    self.handle_authentication(user, identifier)
                else:
                    self.get_existing_user(user)
        else:
            self.create_new_user()
        

app = webapp2.WSGIApplication([('/api/auth/', AuthPage)],
                              debug=True)
                              
