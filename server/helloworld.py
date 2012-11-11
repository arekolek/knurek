import webapp2

import pylast

from lastapikeys import API_KEY, API_SECRET

class MainPage(webapp2.RequestHandler):
  def get(self):
    network = pylast.LastFMNetwork(api_key=API_KEY,api_secret=API_SECRET)  
    self.response.headers['Content-Type'] = 'text/html'
    self.response.write('Hello, webapp2 World! <img src="'+network.get_artist("Air").get_cover_image()+'" />')

app = webapp2.WSGIApplication([('/', MainPage)],
                              debug=True)

