
from google.appengine.ext import db

class Knurek(db.Model):
    name = db.StringProperty()
    session = db.StringProperty()
    active = db.BooleanProperty()

class Friend(db.Model):
    name = db.StringProperty()
    real_name = db.StringProperty()
    image = db.BlobProperty(default=None)

