
import logging

from google.appengine.ext import db, deferred
from google.appengine.api import urlfetch, images

from lib import pylast

from src.lastapikeys import API_KEY, API_SECRET
from src import model

FETCH_LIMIT = 1000


def fetch_avatar(friend):
    logging.info('downloading {0}'.format(friend.name))
    original = urlfetch.Fetch(friend.image_url).content
    im = images.Image(original)
    w = float(im.width)
    h = float(im.height)
    im.crop(0., 0., min(1., h/w), min(1.0, w/h))
    friend.image = db.Blob(im.execute_transforms(output_encoding=images.JPEG))


def fetch_avatars(keys):
    friends = model.Friend.get(keys)
    for f in friends:
        fetch_avatar(f)
    logging.info('storing {0} avatars in datastore'.format(len(friends)))
    db.put(friends)


def fetch_from_lastfm(key):
    account = model.Account.get(key)
    if account:
        logging.info('import friends for user {0}'.format(account.name))
        network = pylast.LastFMNetwork(api_key=API_KEY, api_secret=API_SECRET)
        friends = network.get_user(account.name).get_friends(limit=FETCH_LIMIT)
        updated = []
        for f in friends:
            friend = model.Friend.get_or_insert(key_name=f.get_name(), parent=account)
            friend.name = f.get_name()
            friend.real_name = f.get_real_name()
            friend.image_url = f.get_image()
            friend.account = account
            updated.append(friend)
            logging.info('appended {0}'.format(friend.name))
        logging.info('saving {0} records'.format(len(updated)))
        if updated:
            db.put(updated)
            needUpdate = []
            for f in updated:
                if f.image_url and f.image == None:
                    logging.info('queuing avatar for download {0}'.format(f.name))
                    needUpdate.append(f.key())
            if needUpdate:
                logging.info('deferring download task')
                deferred.defer(fetch_avatars, needUpdate)

