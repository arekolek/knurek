
from google.appengine.ext import db
from google.appengine.api import urlfetch

from lib import pylast

from src.lastapikeys import API_KEY, API_SECRET
from src.model import Friend, Knurek
import logging

FETCH_LIMIT = 1000

def fetch_from_lastfm(identifier):
    user = Knurek.get_by_id(identifier)
    if user and user.session:
        logging.info('import friends for user {0}'.format(user.name))
        network = pylast.LastFMNetwork(api_key=API_KEY, api_secret=API_SECRET)
        friends = network.get_user(user.name).get_friends(limit=FETCH_LIMIT)
        for f in friends:
            friend = Friend.get_or_insert(key_name=f.get_name(), parent=user)
            friend.name = f.get_name()
            friend.real_name = f.get_real_name()
            if f.get_image():
                friend.image = db.Blob(urlfetch.Fetch(f.get_image()).content)
            friend.put()


