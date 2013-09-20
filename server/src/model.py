
import time
from google.appengine.ext import db


def timestamp():
    return int(time.time())


class Account(db.Model):
    name = db.StringProperty()
    session = db.StringProperty()


class Device(db.Model):
    active = db.BooleanProperty(default=False)
    account = db.ReferenceProperty(Account, collection_name='devices')


class Friend(db.Model):
    name = db.StringProperty()
    real_name = db.StringProperty()
    image_url = db.URLProperty(default=None)
    image = db.BlobProperty(default=None)
    account = db.ReferenceProperty(Account, collection_name='friends')
    created = db.DateTimeProperty(auto_now_add=True)
    updated = db.DateTimeProperty(auto_now=True)

