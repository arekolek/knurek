import webapp2, json, os
import jinja2  # @UnresolvedImport

from google.appengine.ext import deferred

from lib import pylast
from src import friends_import
from src.lastapikeys import API_KEY, API_SECRET
from src import model
from datetime import datetime

JINJA_ENVIRONMENT = jinja2.Environment(
    loader=jinja2.FileSystemLoader(os.path.join(os.path.dirname(__file__), '../templates')),
    extensions=['jinja2.ext.autoescape'])

def get_param(key, request):
    return request.headers[key] if key in request.headers else request.get(key) 


def get_int(key, request):
    value = get_param(key, request)
    return int(value) if value else None


def get_float(key, request):
    value = get_param(key, request)
    return float(value) if value else None


def get_datetime(key, request):
    value = get_float(key, request)
    return datetime.fromtimestamp(value) if value else None


class AuthPage(webapp2.RequestHandler):
    def new_device(self):
        key = model.Device().put()
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(json.dumps({"identifier": key.id()}))
        

    def authenticate_device(self, device, identifier):
        network = pylast.LastFMNetwork(api_key=API_KEY, api_secret=API_SECRET)
        keyGen = pylast.SessionKeyGenerator(network)
        if 'token' not in self.request.GET:
            # redirect to api auth with callback here
            callback = '{0}?identifier={1}'.format(self.request.path_url, identifier) 
            self.redirect(keyGen.get_web_auth_url(callback))
        else:
            # get session
            # show a webpage telling to close the browser
            # import contacts
            token = self.request.GET['token']
            result = keyGen.get_web_auth_session_key(token)
            
            account = model.Account.get_or_insert(key_name=result['name'])
            account.name = result['name']
            account.session = result['key']
            account.put()
            
            device.account = account
            device.put()
            
            self.response.headers['Content-Type'] = 'text/html'
            template = JINJA_ENVIRONMENT.get_template('finish.html')
            self.response.write(template.render({'account': account}))
            
            deferred.defer(friends_import.fetch_from_lastfm, account.key())
    
    
    def activate_device(self, device):
        device.active = True
        device.put()
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(json.dumps({"name": device.account.name}))
    
    
    def handle_not_found(self, identifier):
        self.response.write('No such device')
    
    
    def get(self):
        if 'identifier' in self.request.GET:
            identifier = get_int('identifier', self.request)
            device = model.Device.get_by_id(identifier)
            if device:
                if device.account:
                    self.activate_device(device)
                else:
                    self.authenticate_device(device, identifier)
            else:
                self.handle_not_found(identifier)
        else:
            self.new_device()
        

app = webapp2.WSGIApplication([('/api/auth/', AuthPage)],
                              debug=True)
                              
