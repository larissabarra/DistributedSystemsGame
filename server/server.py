from flask import Flask, render_template, request, jsonify
import json
import paho.mqtt.client as mqtt

app = Flask(__name__)
data = []

@app.route('/')
def capture():
    return render_template ('index.html')

@app.route('/getData')
def getData():
    return jsonify(data)

# paho callbacks
def on_connect(client, userdata, flags, rc):
    # Start subscribe, with QoS level 2
    client.subscribe("coordenadas", 2)
    print("rc: " + str(rc))

def on_subscribe(mosq, obj, mid, granted_qos):
    print("Subscribed: " + str(mid) + " " + str(granted_qos))

def on_message(mosq, obj, msg):
    message = msg.payload.decode("utf-8")
    print(msg.topic + " " + str(msg.qos) + " " + message)
    message_json = json.loads(message)
    print(message_json)
    data.append({'sensor': message_json['sensor'], 'location': message_json['location']})

if __name__=='__main__':
    client = mqtt.Client()
    client.on_connect = on_connect
    client.on_subscribe = on_subscribe
    client.on_message = on_message
    client.connect("127.0.0.1", 1883, 60)
    client.loop_start()

    app.run(host="127.0.0.1", port=8080)
