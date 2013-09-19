
import webapp2
import logging

from src import model

class AvatarPage(webapp2.RequestHandler):
    
    def get(self, friend_name):
        identifier = int(self.request.headers['Identifier'])
        user = model.Knurek.get_by_id(identifier)
        if user and user.session:
            friend = model.Friend.get_by_key_name(friend_name, user)
            if friend:
                self.response.headers['Content-Type'] = 'image/jpeg'
                self.response.write(friend.image)
            else:
                logging.error('friend: {0} not found'.format(friend_name))
        else:
            logging.error('user not found')


app = webapp2.WSGIApplication([('/api/friends/(.*)', AvatarPage)],
                              debug=True)

