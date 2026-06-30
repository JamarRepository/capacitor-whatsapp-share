import { Component, OnInit } from '@angular/core';
import { AlertController, ToastController } from '@ionic/angular';
import { WhatsAppShare, WhatsAppApp, Base64File } from 'capacitor-whatsapp-share';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
})
export class HomePage implements OnInit {
  selectedApp: WhatsAppApp = 'whatsapp';

  waInstalled  = false;
  wabInstalled = false;

  // Share to contact
  jid            = '';
  contactMessage = '';
  sendingContact = false;

  // Share file(s)
  selectedFiles: File[]    = [];
  selectedFilePaths: string[] = [];
  fileMessage    = '';
  sendingFile    = false;

  // Share Base64
  base64Files: Base64File[] = [];
  base64Message = '';
  sendingBase64 = false;

  constructor(
    private toastCtrl: ToastController,
    private alertCtrl: AlertController,
  ) {}

  async ngOnInit() {
    await this.checkInstallation();
  }

  async checkInstallation() {
    const [wa, wab] = await Promise.all([
      WhatsAppShare.isInstalled({ app: 'whatsapp' }),
      WhatsAppShare.isInstalled({ app: 'whatsapp_business' }),
    ]);
    this.waInstalled  = wa.value;
    this.wabInstalled = wab.value;
  }

  // ---------------------------------------------------------------------------
  // Share to contact
  // ---------------------------------------------------------------------------

  async shareToContact() {
    if (!this.jid.trim())            return this.showToast('Ingresa el JID o número', 'warning');
    if (!this.contactMessage.trim()) return this.showToast('Ingresa un mensaje', 'warning');

    this.sendingContact = true;
    try {
      await WhatsAppShare.shareToContact({
        jid: this.jid.trim(),
        message: this.contactMessage.trim(),
        app: this.selectedApp,
      });
    } catch (err: any) {
      this.showAlert('Error', err?.message ?? 'No se pudo abrir WhatsApp');
    } finally {
      this.sendingContact = false;
    }
  }

  // ---------------------------------------------------------------------------
  // Share file(s)
  // ---------------------------------------------------------------------------

  onFilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    this.selectedFiles = Array.from(input.files);
    // En un dispositivo real con Capacitor se obtiene el path o content URI
    // del file picker nativo. En web usamos el nombre como referencia visual.
    this.selectedFilePaths = this.selectedFiles.map(
      (f) => (f as any).path ?? `/storage/emulated/0/Download/${f.name}`,
    );
  }

  removeFilePath(index: number) {
    this.selectedFiles.splice(index, 1);
    this.selectedFilePaths.splice(index, 1);
  }

  async shareFiles() {
    if (!this.selectedFilePaths.length) return this.showToast('Selecciona al menos un archivo', 'warning');

    this.sendingFile = true;
    try {
      await WhatsAppShare.shareFile({
        filePaths: this.selectedFilePaths,
        message: this.fileMessage.trim() || undefined,
        app: this.selectedApp,
      });
    } catch (err: any) {
      this.showAlert('Error', err?.message ?? 'No se pudo compartir');
    } finally {
      this.sendingFile = false;
    }
  }

  // ---------------------------------------------------------------------------
  // Share Base64
  // ---------------------------------------------------------------------------

  onBase64FilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    Array.from(input.files).forEach((file) => {
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = reader.result as string;
        // Quitar el prefijo "data:mime/type;base64,"
        const base64  = dataUrl.split(',')[1];
        this.base64Files.push({
          base64,
          fileName: file.name,
          mimeType: file.type || undefined,
        });
      };
      reader.readAsDataURL(file);
    });
  }

  removeBase64File(index: number) {
    this.base64Files.splice(index, 1);
  }

  async shareBase64() {
    if (!this.base64Files.length) return this.showToast('Selecciona al menos un archivo', 'warning');

    this.sendingBase64 = true;
    try {
      await WhatsAppShare.shareFileFromBase64({
        files: this.base64Files,
        message: this.base64Message.trim() || undefined,
        app: this.selectedApp,
      });
    } catch (err: any) {
      this.showAlert('Error', err?.message ?? 'No se pudo compartir');
    } finally {
      this.sendingBase64 = false;
    }
  }

  // ---------------------------------------------------------------------------

  get appLabel()             { return this.selectedApp === 'whatsapp' ? 'WhatsApp' : 'WhatsApp Business'; }
  get currentAppInstalled()  { return this.selectedApp === 'whatsapp' ? this.waInstalled : this.wabInstalled; }

  private async showToast(message: string, color: string) {
    const t = await this.toastCtrl.create({ message, duration: 2500, color, position: 'bottom' });
    await t.present();
  }

  private async showAlert(header: string, message: string) {
    const a = await this.alertCtrl.create({ header, message, buttons: ['OK'] });
    await a.present();
  }
}
