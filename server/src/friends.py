
import webapp2
import json
import logging

from google.appengine.ext import deferred

from src import friends_import, model, auth
from datetime import datetime

class FriendsPage(webapp2.RequestHandler):
    
    def get_friends(self, account, timestamp, now):
        def get_updated():
            return account.friends.filter('updated > ', last).filter('updated <= ', now)
        last = datetime.fromtimestamp(float(timestamp)) if timestamp else datetime.fromtimestamp(0)
        nonDeleted = get_updated().filter('deleted == ', False)
        deletedCandidates = get_updated().filter('deleted == ', True)
        created = [f for f in nonDeleted if f.created > last]
        updated = [f for f in nonDeleted if f.created <= last]
        deleted = [f for f in deletedCandidates if f.created <= last]
        logging.info('friends for {0} since {1}'.format(account.name, last))
        logging.info('there are {0} created'.format(len(created)))
        logging.info('there are {0} updated'.format(len(updated)))
        logging.info('there are {0} deleted'.format(len(deleted)))
        output = {
                  'created': [{
                               'name': f.name,
                               'real_name': f.real_name,
                               'image': f.image != None,
                               } for f in created ],
                  'updated': [{
                               'name': f.name,
                               'real_name': f.real_name,
                               'image': f.image != None,
                               } for f in updated ],
                  'deleted': [{
                               'name': f.name,
                               } for f in deleted ],
                  'timestamp': model.timestamp(now)
                  }
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(json.dumps(output))
    
    
    def post(self, timestamp):
        logging.info('body: {0}'.format(self.request.body))
        identifier = auth.get_int('identifier', self.request)
        device = model.Device.get_by_id(identifier)
        if device:
            self.get_friends(device.account, timestamp, datetime.now())
            deferred.defer(friends_import.fetch_from_lastfm, device.account.key())


app = webapp2.WSGIApplication([('/api/friends/sync/(.*)', FriendsPage)],
                              debug=True)

