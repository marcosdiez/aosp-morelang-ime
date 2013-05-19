#!/bin/bash
cp ../java/bin/java-release-unsigned.apk .
jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore CertificateMarcosDiezAndroid.key java-release-unsigned.apk "marcos diez"
