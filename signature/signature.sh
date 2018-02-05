#!/bin/sh

./keytool-importkeypair -k sys_keystore.jks -p Aa123456 -pk8 platform.pk8 -cert platform.x509.pem -alias as_keys0
