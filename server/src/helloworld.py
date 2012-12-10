import webapp2

import json

from lib import pylast

from lastapikeys import API_KEY, API_SECRET

class MainPage(webapp2.RequestHandler):
    def get(self):
        network = pylast.LastFMNetwork(api_key=API_KEY,api_secret=API_SECRET)  
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(json.dumps([{'name':f.get_name(),'realname':f.get_real_name(),'image':f.get_image()} for f in network.get_user(self.request.GET['user']).get_friends()[:10]]))

app = webapp2.WSGIApplication([('/', MainPage)],
                              debug=True)

