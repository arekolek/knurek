import webapp2, json

from google.appengine.ext import db
import base64
import os
from lastapikeys import API_KEY
import urllib

class User(db.Model):
    name = db.StringProperty()
    token = db.StringProperty()
    session = db.StringProperty()
    identifier = db.StringProperty()

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
                    if 'token' not in self.request.GET:
                        # redirect to api auth with callback here
                        callback = 'http://' + self.request.headers['Host'] + '/api/auth?identifier=' + identifier 
                        url = 'http://www.last.fm/api/auth?' + urllib.urlencode({'api_key': API_KEY, 'cb': callback})
                        self.redirect(url)
                    else:
                        # get session
                        # show a webpage telling to close the browser
                        pass
                else:
                    # return username
                    pass
        else:
            # create new user
            identifier =  base64.urlsafe_b64encode(os.urandom(30))
            User(identifier=identifier).put()
            
            self.response.headers['Content-Type'] = 'application/json'
            self.response.write(json.dumps({"identifier": identifier}))
        

app = webapp2.WSGIApplication([('/api/auth/', AuthPage)],
                              debug=True)
                              
