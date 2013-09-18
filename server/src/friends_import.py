
from google.appengine.ext import db, deferred
from google.appengine.api import urlfetch, images

from lib import pylast

from src.lastapikeys import API_KEY, API_SECRET
from src.model import Friend, Knurek
import logging

FETCH_LIMIT = 1000


def fetch_avatar(key, url):
    friend = Friend.get(key)
    logging.debug('downloading {0}'.format(friend.name))
    original = urlfetch.Fetch(url).content
    im = images.Image(original)
    w = float(im.width)
    h = float(im.height)
    im.crop(0., 0., min(1., h/w), min(1.0, w/h))
    friend.image = db.Blob(im.execute_transforms(output_encoding=images.JPEG))
    friend.put()


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
            friend.put()
            logging.debug('added {0}'.format(friend.name))
            if f.get_image():
                logging.debug('queuing for download {0}'.format(friend.name))
                deferred.defer(fetch_avatar, friend.key(), f.get_image())

