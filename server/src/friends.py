import webapp2

import json

from lib import pylast

from lastapikeys import API_KEY, API_SECRET
from src.auth import User

class FriendsPage(webapp2.RequestHandler):
    def get(self):
        identifier = self.request.headers['Identifier']
        user = User.all().filter("identifier = ", identifier).get()
        if user.session != None:
            network = pylast.LastFMNetwork(api_key=API_KEY, api_secret=API_SECRET)
            friends = network.get_user(user.name).get_friends()
            output = {'friends': [{'name': f.get_name(), 'realname': f.get_real_name(), 'image': f.get_image()} for f in friends]}
            self.response.headers['Content-Type'] = 'application/json'
            self.response.write(json.dumps(output))

app = webapp2.WSGIApplication([('/api/friends/', FriendsPage)],
                              debug=True)

