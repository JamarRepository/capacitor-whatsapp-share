package com.mueblesjamar.whatsappshareexample;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import com.mueblesjamar.whatsappshare.WhatsAppSharePlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WhatsAppSharePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
