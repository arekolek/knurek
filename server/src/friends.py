
import webapp2
import json

from google.appengine.ext import deferred

from src import friends_import, model

class FriendsPage(webapp2.RequestHandler):
    
    def get_friends(self, account):
        friends = account.friends
        output = {'friends': [{
                    'name': f.name,
                    'real_name': f.real_name,
                    'image': f.image != None
                    } for f in friends ]}
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(json.dumps(output))
    
    
    def get(self):
        identifier = int(self.request.headers['Identifier'])
        device = model.Device.get_by_id(identifier)
        if device:
            self.get_friends(device.account)
            deferred.defer(friends_import.fetch_from_lastfm, device.account.key())


app = webapp2.WSGIApplication([('/api/friends/', FriendsPage)],
                              debug=True)

