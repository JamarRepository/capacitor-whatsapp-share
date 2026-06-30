import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.mueblesjamar.whatsappshareexample',
  appName: 'WhatsApp Share Demo',
  webDir: 'www',
  android: {
    buildOptions: {
      keystorePath: undefined,
    },
  },
};

export default config;
