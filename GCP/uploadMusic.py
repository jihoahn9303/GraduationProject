from google.oauth2 import service_account
from google.cloud import storage
import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
import os 
import glob
from datetime import datetime

# authentication for using firebase realtime base
cred = credentials.Certificate('./credentials/firebase_admin_serviceAccountKey.json')  # input your firebase admin sdk json file path
firebase_admin.initialize_app(cred, {
    'databaseURL' : 'https://smartcafe-286310.firebaseio.com/'
})

# autentication for using google cloud storage
credentials_for_storage = service_account.Credentials.from_service_account_file('./credentials/smartcafe-286310-7630ecb69883.json')  # input your service account json file path
scoped_credentials = credentials_for_storage.with_scopes(['https://www.googleapis.com/auth/cloud-platform'])
client = storage.Client(project='smartcafe-286310', credentials=credentials_for_storage)


def uploadMusic(bucket_name, source_file_name, destination_blob_name, category):
    bucket = client.bucket(bucket_name)
    blob = bucket.blob(destination_blob_name)
    blob.upload_from_filename(source_file_name)
    blob.make_public()
    
    song_name = (source_file_name.split('/')[-1]).split('.')[0]
    uploadDetailsToDatabase(song_name, blob.public_url, category)
    
    return blob.public_url

# upload details for music file to the firebase realtime database
def uploadDetailsToDatabase(song_name, song_uri, category):
    ref = db.reference().child('Songs').child(category)
    ref.push({'songName' : song_name, 'songUri' : song_uri})

path = "./music_for_gcp/"  # music root folder
for category in ['cold', 'hot', 'medium', 'white']:
    new_path = path + category
    file_list = os.listdir(new_path)
    
    for file in file_list:
        destination_blob_name = 'Songs' + '/' + category + '/' + file.split('.')[0]   # destination path(for music file)
        source_file_name = new_path + '/' + file    # source file path in your local
        uploadMusic('smartcafe-286310.appspot.com', source_file_name, destination_blob_name, category)  # upload music file to google cloud storage


