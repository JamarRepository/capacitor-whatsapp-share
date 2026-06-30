export type WhatsAppApp = 'whatsapp' | 'whatsapp_business';

export interface ShareToContactOptions {
  /**
   * JID of the WhatsApp contact.
   * Accepted formats:
   *   - Full JID:    "5491112345678@s.whatsapp.net"
   *   - Phone only:  "5491112345678"  (international format, no + or spaces)
   */
  jid: string;

  /** Text message to send. */
  message: string;

  /** Which WhatsApp app to open. Defaults to 'whatsapp'. */
  app?: WhatsAppApp;
}

export interface ShareFileOptions {
  /**
   * One or more file paths / content URIs to share.
   *
   * Accepted values per entry:
   *   - Absolute path:  "/storage/emulated/0/Download/factura.pdf"
   *   - Content URI:    "content://com.android.providers.downloads.documents/document/123"
   *
   * On Android, files selected from the system picker arrive as content:// URIs.
   * Both formats are supported. Pass multiple entries to share several files at once.
   */
  filePaths: string[];

  /**
   * MIME type that applies to all files, e.g. "image/jpeg", "application/pdf".
   * Use "image/*" for mixed images. Defaults to any type.
   * When sharing multiple files of different types, omit this and let the plugin
   * detect the common type automatically.
   */
  mimeType?: string;

  /** Optional caption text sent alongside the file(s). */
  message?: string;

  /**
   * Which WhatsApp app to open. Defaults to 'whatsapp'.
   * WhatsApp always shows its own contact picker when sharing files.
   */
  app?: WhatsAppApp;
}

export interface Base64File {
  /**
   * File content encoded as Base64 (without data URI prefix).
   * Example: "JVBERi0xLjQK..." (not "data:application/pdf;base64,...")
   */
  base64: string;

  /**
   * File name including extension, e.g. "factura.pdf", "foto.jpg".
   * Used as the temporary file name on disk.
   */
  fileName: string;

  /** MIME type for this specific file, e.g. "image/jpeg", "application/pdf". */
  mimeType?: string;
}

export interface ShareFileFromBase64Options {
  /**
   * One or more files encoded as Base64 to share.
   * Each entry becomes a temporary file that is shared to WhatsApp.
   */
  files: Base64File[];

  /** Optional caption text sent alongside the file(s). */
  message?: string;

  /**
   * Which WhatsApp app to open. Defaults to 'whatsapp'.
   * WhatsApp always shows its own contact picker when sharing files.
   */
  app?: WhatsAppApp;
}

export interface WhatsAppSharePlugin {
  /**
   * Open a specific WhatsApp (or WhatsApp Business) chat and pre-fill the
   * given message for the user to send.
   *
   * @since 1.0.0
   */
  shareToContact(options: ShareToContactOptions): Promise<void>;

  /**
   * Share one or more files (by path or content URI) to WhatsApp.
   * WhatsApp will show its own contact/chat picker.
   *
   * @since 1.0.0
   */
  shareFile(options: ShareFileOptions): Promise<void>;

  /**
   * Decode one or more Base64 strings into temporary files and share them
   * to WhatsApp. Useful when files come from an API response.
   *
   * @since 1.0.0
   */
  shareFileFromBase64(options: ShareFileFromBase64Options): Promise<void>;

  /**
   * Check whether a WhatsApp app is installed on the device.
   *
   * @since 1.0.0
   */
  isInstalled(options?: { app?: WhatsAppApp }): Promise<{ value: boolean }>;
}
