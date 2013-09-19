
import webapp2
import json

from google.appengine.ext import deferred

from src import friends_import, model

class FriendsPage(webapp2.RequestHandler):
    
    def get_friends(self, user):
        friends = model.Friend.all().ancestor(user.key())
        output = {'friends': [{
                    'name': f.name,
                    'real_name': f.real_name,
                    'image': f.image != None
                    } for f in friends ]}
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(json.dumps(output))
    
    
    def get(self):
        identifier = int(self.request.headers['Identifier'])
        user = model.Knurek.get_by_id(identifier)
        if user and user.session:
            self.get_friends(user)
        deferred.defer(friends_import.fetch_from_lastfm, identifier)


app = webapp2.WSGIApplication([('/api/friends/', FriendsPage)],
                              debug=True)

