
import webapp2
import logging

from src import model, auth

class AvatarPage(webapp2.RequestHandler):
    
    def get(self, friend_name):
        identifier = auth.get_int('identifier', self.request)
        device = model.Device.get_by_id(identifier)
        if device:
            friend = device.account.friends.filter('name = ', friend_name).get()
            if friend:
                self.response.headers['Content-Type'] = 'image/jpeg'
                self.response.write(friend.image)
            else:
                logging.error('friend: {0} not found'.format(friend_name))
        else:
            logging.error('user not found')


app = webapp2.WSGIApplication([('/api/friends/(.*)', AvatarPage)],
                              debug=True)

