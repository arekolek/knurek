
import logging

from google.appengine.ext import db, deferred
from google.appengine.api import urlfetch, images

from lib import pylast

from src.lastapikeys import API_KEY, API_SECRET
from src import model
from datetime import datetime

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


def update(friend, f):
    changed = False
    if friend.deleted:
        friend.created = datetime.now()
        friend.deleted = False
        changed = True
    if friend.real_name != f.get_real_name():
        friend.real_name = f.get_real_name()
        changed = True 
    if friend.image_url != f.get_image():
        friend.image_url = f.get_image()
        changed = True
    return changed


def get_friend(f, a):
    return model.Friend.get_or_insert(key_name=f.get_name(), parent=a, 
                                      account=a, name=f.get_name())


def fetch_from_lastfm(key):
    account = model.Account.get(key)
    if account:
        logging.info('import friends for user {0}'.format(account.name))
        network = pylast.LastFMNetwork(api_key=API_KEY, api_secret=API_SECRET)
        friends = network.get_user(account.name).get_friends(limit=FETCH_LIMIT)
        
        friend_names = {f.name for f in friends}
        deleted = []
        for friend in account.friends.filter('deleted == ', False):
            if friend.name not in friend_names:
                friend.soft_delete(False)
                deleted.append(friend)
        logging.info('soft deleting {0} friends'.format(len(deleted)))
        db.put(deleted)
        
        updated = []
        for lfmf in friends:
            friend = get_friend(lfmf, account)
            if update(friend, lfmf):
                updated.append(friend)
                logging.info('updating {0}'.format(friend.name))
        logging.info('updating {0} friends'.format(len(updated)))
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
        
