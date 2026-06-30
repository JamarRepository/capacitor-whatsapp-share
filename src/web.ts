import { WebPlugin } from '@capacitor/core';

import type {
  ShareFileFromBase64Options,
  ShareFileOptions,
  ShareToContactOptions,
  WhatsAppSharePlugin,
} from './definitions';

export class WhatsAppShareWeb extends WebPlugin implements WhatsAppSharePlugin {
  async shareToContact(options: ShareToContactOptions): Promise<void> {
    const phone = extractPhone(options.jid);
    const encoded = encodeURIComponent(options.message);
    window.open(`https://wa.me/${phone}?text=${encoded}`, '_blank');
  }

  async shareFile(_options: ShareFileOptions): Promise<void> {
    throw this.unimplemented('shareFile is not supported on the web.');
  }

  async shareFileFromBase64(_options: ShareFileFromBase64Options): Promise<void> {
    throw this.unimplemented('shareFileFromBase64 is not supported on the web.');
  }

  async isInstalled(): Promise<{ value: boolean }> {
    return { value: false };
  }
}

function extractPhone(jid: string): string {
  return jid.includes('@') ? jid.split('@')[0] : jid;
}
