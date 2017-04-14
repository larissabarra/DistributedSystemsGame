# DistributedSystemsGame

Usar dependências:

dependencies {
    compile fileTree(include: ['*.jar'], dir: 'libs')
    testCompile 'junit:junit:4.12'
    compile 'com.android.support:appcompat-v7:23.0.1'
    compile('com.google.android.gms:play-services-maps:10.2.1') {
        exclude module: 'support-v4'
    }
    compile('com.google.android.gms:play-services-location:10.2.1') {
        exclude module: 'support-v4'
    }
    compile 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.0.2'
    compile('org.eclipse.paho:org.eclipse.paho.android.service:1.0.2') {
        exclude module: 'support-v4'
    }
}
