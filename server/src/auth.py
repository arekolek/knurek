import webapp2, json

from google.appengine.ext import db
import base64
import os

class User(db.Model):
    name = db.StringProperty()
    token = db.StringProperty()
    session = db.StringProperty()
    identifier = db.StringProperty()

class AuthPage(webapp2.RequestHandler):
    def get(self):
        self.response.headers['Content-Type'] = 'application/json'
        if 'identifier' in self.request.GET:
            identifier = self.request.GET['identifier']
            user = User.all().filter("identifier = ", identifier).get()
            if user == None:
                # error
                pass
            else:
                if user.session == None:
                    if 'token' not in self.request.GET['token']:
                        # redirect to api auth with callback here
                        pass
                    else:
                        # get session
                        # show a webpage telling to close the browser
                        pass
                else:
                    # return username
                    pass
            pass
        else:
            # create new user
            identifier =  base64.urlsafe_b64encode(os.urandom(30))
            User(identifier=identifier).put()
            self.response.write(json.dumps({"identifier": identifier}))
            pass
        
        

app = webapp2.WSGIApplication([('/api/auth/', AuthPage)],
                              debug=True)
                              
