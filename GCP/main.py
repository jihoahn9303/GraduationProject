import base64
from google.oauth2 import service_account
from google.cloud import storage
import requests
import numpy as np 
import cv2
from sklearn.cluster import KMeans
from collections import Counter
import random
import json
import base64

def image_resize(image, width = None, height = None, inter = cv2.INTER_AREA):
       dim = None
       (h, w) = image.shape[:2]

       if width is None and height is None:
           return image

       if width is None:
           r = height / float(h)
           dim = (int(w * r), height)

       else:
           r = width / float(w)
           dim = (width, int(h * r))

       resized = cv2.resize(image, dim, interpolation = inter)
       return resized

def make_bar(height, width, color):
    bar = np.zeros((height, width, 3), np.uint8)
    bar[:] = color
    red, green, blue = int(color[2]), int(color[1]), int(color[0])
    hsv_bar = cv2.cvtColor(bar, cv2.COLOR_BGR2HSV)
    hue, sat, val = hsv_bar[0][0]
    return bar, (red, green, blue), (hue, sat, val)

def sort_hsvs(hsv_list):
    bars_with_indexes = []
    for index, hsv_val in enumerate(hsv_list):
        bars_with_indexes.append((index, hsv_val[0], hsv_val[1], hsv_val[2]))
    bars_with_indexes.sort(key=lambda elem: (elem[1], elem[2], elem[3]))
    return [item[0] for item in bars_with_indexes]

def calculate_hsv(cluster):
    array = list()
    cluster_centers = cluster.cluster_centers_
    for BGRArray in cluster_centers:
        _,_,hsv_tuple = make_bar(100, 100, BGRArray)
        hsv_array = list(hsv_tuple)
        array.append(hsv_array)
    return array

def calculate_proportion(cluster_count_list) :
    proArray = list()
    totalValue = 0
    for element in cluster_count_list:
        totalValue += element
    for element in cluster_count_list:
        proportion = round(element / totalValue * 100, 1)        
        proArray.append(proportion)
    return proArray

def calculate_category_score(cluster, cluster_counter_list):
    hsv_array = calculate_hsv(cluster)
    category_list = list()
    proportion_list = list()
    mColor_score, hColor_score, wColor_score, cColor_score = float(0), float(0), float(0), float(0)
    
    #해당 색상이 어떠한 색상계에 속하는 지 판단하는 부분
    for element in hsv_array:
        h, s, v = element[0], element[1], element[2]
        if s <= 10 and s >= 0 :
            if v >= 0 and v< 86 :
                category_list.append("m_color")
            elif v>=86 and v<199 :
                category_list.append("c_color")
            else :
                category_list.append("w_color")
        else :
            if v < 63 :
                category_list.append("m_color")
            else : 
                if h>=0 and h<=30 :
                    category_list.append("h_color")
                elif h>30 and h<=74:
                    category_list.append("m_color")
                elif h>74 and h<=135:
                    category_list.append("c_color")
                elif h>135 and h<=164:
                    category_list.append("m_color")
                else :
                    category_list.append("h_color")
     
    # 각 색계열 카테고리에 대해 순위 점수 계산   
    proportion_array = calculate_proportion(cluster_counter_list)
    for index, category in enumerate(category_list):
        if category == "m_color":
            mColor_score += proportion_array[index]
        elif category == "h_color":
            hColor_score += proportion_array[index]
        elif category == "w_color":
            wColor_score += proportion_array[index]
        else :
            cColor_score += proportion_array[index]
    
    return mColor_score, hColor_score, wColor_score, cColor_score

def define_category(score_array):    
    max_score = max(score_array)
    category = ""

    # 동률인 경우
    if score_array.count(max_score) >= 2:
        same_value_index_array = list()    # 색계의 score 값이 같은 경우 색계 index 저장하는 배열
        for index, value in enumerate(score_array):
            if value == max_score:
                same_value_index_array.append(index)
        random_index =  random.choice(same_value_index_array)
        if random_index == 0:
            category = "medium"
        elif random_index == 1:
            category = "cold"
        elif random_index == 2:
            category = "white"
        else :
            category = "hot"
            
    # 동률인 값 없이 하나의 카테고리만 선택된 경우
    else:
        if max_score == score_array[0]:
            category = "medium"
        elif max_score == score_array[1]:
            category = "cold"
        elif max_score == score_array[2]:
            category = "white"
        else :
            category = "hot"

    return category

def make_list_by_dict(cluster_dict):
    ret = list()
    for i in range(len(cluster_dict)):
        ret.append(0)
    for key, values in enumerate(cluster_dict):
        ret[key] = values
    return ret

def callback(message):
    print("Received message: {}".format(message))
    message.ack()

def requests_message(server_key, token, message_body):
    url = 'https://fcm.googleapis.com/fcm/send'
    
    headers = {
        'Authorization': 'key= ' + server_key,
        'Content-Type': 'application/json; UTF-8',
    }

    content = {
        'to': token,
        'priority' : "high",
        # background message
        # 'notification' : {
        #     "title": "Smartcafe Message",
        #     "body" : "Hello, you get {} category!".format(message_body)
        # },
        # foreground message
        'data': {
            "title" : "Smartcafe Message",
            "message" : message_body
        }
    }

    requests.post(url=url, data=json.dumps(content), headers=headers)

def hello_pubsub(event, context):
    print("start google cloud function!")
    message = base64.b64decode(event['data']).decode('utf-8')
    message_dict = json.loads(message)
    credentials_for_storage = service_account.Credentials.from_service_account_file('./smartcafe-286310-7630ecb69883.json')
    scoped_credentials = credentials_for_storage.with_scopes(['https://www.googleapis.com/auth/cloud-platform'])
    client = storage.Client(project='smartcafe-286310', credentials=credentials_for_storage)
    email = message_dict['email']
   
    category = ""
    mColor_score, hColor_score, wColor_score, cColor_score = 0, 0, 0, 0  # 중성계열, 난색계열, 흰색계열, 한색계열
   
    blobs = client.list_blobs('smartcafe-286310.appspot.com', prefix=email)
    source_bucket = client.get_bucket('smartcafe-286310.appspot.com')
    
    for blob in blobs:
        source_blob = source_bucket.get_blob(blob.name)
        image = np.asarray(bytearray(source_blob.download_as_string()), dtype="uint8")
        image = cv2.imdecode(image, cv2.IMREAD_COLOR)
        
        # image processing
        height, width, _ = np.shape(image)
        if height > 720:
            image = image_resize(image, height = 720)
        elif width > 720:
            image = image_resize(image, width = 720)
        height, width, _ = np.shape(image)
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        image = image.reshape((height * width, 3))

        # K-means clustering
        clusters = KMeans(n_clusters=4)
        cluster_result = clusters.fit_predict(image)
        cluster_result_dict = Counter(cluster_result)

        # change cluster_result_dict to list
        ret_list = make_list_by_dict(cluster_result_dict)

        # calculate score of each categories
        m, h, w, c = calculate_category_score(clusters, ret_list)
        mColor_score += m
        hColor_score += h
        wColor_score += w
        cColor_score += c

        # delete images in given folder
        blob.delete()

    #  define category
    score_array = [mColor_score, cColor_score, wColor_score, hColor_score]
    category = define_category(score_array)
    print(category)
    print(score_array)

    # publish category message to topic using FCM App pushing
    while category == "":
        pass
    server_key = "..." # your FCM App server key
    # message_body = "Hello, you get {} category!".format(category)
    message_body = category
    token = message_dict['token']
    requests_message(server_key, token, message_body)
