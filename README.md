# capacitor-whatsapp-share

Plugin de Capacitor para compartir mensajes y archivos directamente a WhatsApp o WhatsApp Business desde aplicaciones Android. Soporta apertura de chats específicos por JID, compartir archivos por ruta/URI, múltiples archivos y envío desde Base64.

---

## Índice

- [Instalación](#instalación)
- [Configuración Android](#configuración-android)
- [Uso](#uso)
  - [shareToContact](#sharetocontact)
  - [shareFile](#sharefile)
  - [shareFileFromBase64](#sharefilefrombase64)
  - [isInstalled](#isinstalled)
- [API completa](#api-completa)
- [Cómo modificar el plugin](#cómo-modificar-el-plugin)

---

## Instalación

### Requisitos

- Node.js >= 18
- Capacitor >= 6
- Android SDK >= 22

### Desde repositorio Git privado

```bash
# Última versión de la rama main
npm install git+https://github.com/tu-empresa/capacitor-whatsapp-share.git

# Versión específica por tag
npm install git+https://github.com/tu-empresa/capacitor-whatsapp-share.git#v1.0.0

# Sincronizar con Android
npx cap sync android
```

> Reemplaza `tu-empresa/capacitor-whatsapp-share` con la ruta real de tu repositorio.

---

## Configuración Android

Abre `android/app/src/main/java/<tu-paquete>/MainActivity.java` y registra el plugin **antes** de `super.onCreate()`:

```java
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
```

> Este paso es manual y se hace **una sola vez** por proyecto. El `npx cap sync` actualiza el `build.gradle` automáticamente, pero el registro en `MainActivity` siempre es manual en Android.

---

## Uso

Importa el plugin en tu archivo TypeScript/JavaScript:

```typescript
import { WhatsAppShare } from 'capacitor-whatsapp-share';
```

---

### shareToContact

Abre un chat específico de WhatsApp con un mensaje pre-cargado usando el JID o número de teléfono del contacto.

```typescript
await WhatsAppShare.shareToContact({
  jid: '5491112345678',           // número en formato internacional sin + ni espacios
  message: 'Hola! Te escribo desde la app.',
  app: 'whatsapp',                // 'whatsapp' | 'whatsapp_business'
});
```

**Formatos de JID aceptados:**

| Formato | Ejemplo |
|---|---|
| Número solo | `5491112345678` |
| JID completo | `5491112345678@s.whatsapp.net` |

---

### shareFile

Comparte uno o más archivos a WhatsApp. Acepta rutas absolutas y `content://` URIs (resultado de un file picker nativo).

```typescript
// Un solo archivo
await WhatsAppShare.shareFile({
  filePaths: ['/storage/emulated/0/Download/factura.pdf'],
  mimeType: 'application/pdf',
  message: 'Adjunto tu factura.',
  app: 'whatsapp_business',
});

// Múltiples archivos
await WhatsAppShare.shareFile({
  filePaths: [
    '/storage/emulated/0/DCIM/foto1.jpg',
    '/storage/emulated/0/DCIM/foto2.jpg',
  ],
  mimeType: 'image/jpeg',
  message: 'Te envío las fotos.',
});

// Desde un file picker (content:// URI)
await WhatsAppShare.shareFile({
  filePaths: ['content://com.android.providers.downloads.documents/document/123'],
});
```

> WhatsApp siempre muestra su propio selector de chats al compartir archivos. No es posible apuntar a un contacto específico por JID cuando se comparten archivos — eso es una limitación de WhatsApp.

**Detección automática de MIME type:**

Si omites `mimeType`, el plugin lo detecta por extensión del archivo. Si los archivos tienen tipos distintos, usa `*/*` automáticamente.

---

### shareFileFromBase64

Decodifica uno o más archivos en Base64, los escribe en un directorio temporal y los comparte a WhatsApp. Útil cuando los archivos provienen de una respuesta de API.

```typescript
// Un solo archivo
await WhatsAppShare.shareFileFromBase64({
  files: [
    {
      base64: 'JVBERi0xLjQK...',   // sin prefijo "data:mime;base64,"
      fileName: 'factura.pdf',
      mimeType: 'application/pdf',
    },
  ],
  message: 'Adjunto tu factura.',
  app: 'whatsapp_business',
});

// Múltiples archivos
await WhatsAppShare.shareFileFromBase64({
  files: [
    {
      base64: '/9j/4AAQSkZJRgAB...',
      fileName: 'foto1.jpg',
      mimeType: 'image/jpeg',
    },
    {
      base64: 'JVBERi0xLjQK...',
      fileName: 'contrato.pdf',
      mimeType: 'application/pdf',
    },
  ],
  message: 'Documentos adjuntos.',
});
```

**Cómo obtener el Base64 desde una imagen en Ionic/Angular:**

```typescript
const reader = new FileReader();
reader.onload = async () => {
  const dataUrl = reader.result as string;
  const base64  = dataUrl.split(',')[1]; // quitar "data:image/jpeg;base64,"

  await WhatsAppShare.shareFileFromBase64({
    files: [{ base64, fileName: 'foto.jpg', mimeType: 'image/jpeg' }],
  });
};
reader.readAsDataURL(selectedFile);
```

---

### isInstalled

Verifica si WhatsApp o WhatsApp Business está instalado en el dispositivo.

```typescript
const { value } = await WhatsAppShare.isInstalled({ app: 'whatsapp' });

if (value) {
  console.log('WhatsApp está instalado');
} else {
  console.log('WhatsApp no está instalado');
}

// Verificar ambos
const [wa, wab] = await Promise.all([
  WhatsAppShare.isInstalled({ app: 'whatsapp' }),
  WhatsAppShare.isInstalled({ app: 'whatsapp_business' }),
]);
```

---

## API completa

### `shareToContact(options)`

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `jid` | `string` | Sí | JID o número de teléfono del contacto |
| `message` | `string` | Sí | Texto a pre-cargar en el chat |
| `app` | `WhatsAppApp` | No | `'whatsapp'` (default) o `'whatsapp_business'` |

### `shareFile(options)`

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `filePaths` | `string[]` | Sí | Rutas absolutas o `content://` URIs |
| `mimeType` | `string` | No | MIME type. Se detecta automáticamente si se omite |
| `message` | `string` | No | Texto opcional junto al archivo |
| `app` | `WhatsAppApp` | No | `'whatsapp'` (default) o `'whatsapp_business'` |

### `shareFileFromBase64(options)`

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `files` | `Base64File[]` | Sí | Lista de archivos en Base64 |
| `message` | `string` | No | Texto opcional junto al archivo |
| `app` | `WhatsAppApp` | No | `'whatsapp'` (default) o `'whatsapp_business'` |

**`Base64File`:**

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `base64` | `string` | Sí | Contenido del archivo en Base64 (sin prefijo `data:`) |
| `fileName` | `string` | Sí | Nombre del archivo con extensión, ej: `factura.pdf` |
| `mimeType` | `string` | No | MIME type del archivo |

### `isInstalled(options?)`

| Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|
| `app` | `WhatsAppApp` | No | `'whatsapp'` (default) o `'whatsapp_business'` |

**Retorna:** `Promise<{ value: boolean }>`

---

## Cómo modificar el plugin

### Estructura del proyecto

```
capacitor-whatsapp-share/
├── src/                              # Código TypeScript (API pública)
│   ├── definitions.ts                # Interfaces y tipos
│   ├── index.ts                      # Registro del plugin
│   └── web.ts                        # Implementación web (fallback)
├── android/
│   ├── build.gradle                  # Configuración Gradle del plugin
│   └── src/main/
│       ├── AndroidManifest.xml       # FileProvider y queries de paquetes
│       ├── java/com/mueblesjamar/whatsappshare/
│       │   ├── WhatsAppSharePlugin.kt        # Lógica nativa principal
│       │   └── WhatsAppShareFileProvider.kt  # Subclase FileProvider (evita conflictos)
│       └── res/xml/
│           └── whatsapp_share_file_paths.xml # Rutas accesibles por FileProvider
├── example/                          # App Ionic de ejemplo
├── package.json
├── tsconfig.json
└── rollup.config.mjs
```

### Flujo de desarrollo

```bash
# 1. Clonar el repositorio
git clone https://github.com/tu-empresa/capacitor-whatsapp-share.git
cd capacitor-whatsapp-share

# 2. Instalar dependencias
npm install

# 3. Compilar el plugin (TypeScript → dist/)
npm run build

# 4. Probar cambios en la app de ejemplo
cd example
npm install
npx cap sync android
npx cap open android
```

### Agregar un nuevo método

**Paso 1 — Definir la interfaz en `src/definitions.ts`:**

```typescript
export interface MiNuevaOpcion {
  parametro: string;
}

export interface WhatsAppSharePlugin {
  // ... métodos existentes ...
  miNuevoMetodo(options: MiNuevaOpcion): Promise<void>;
}
```

**Paso 2 — Implementación web en `src/web.ts`:**

```typescript
async miNuevoMetodo(_options: MiNuevaOpcion): Promise<void> {
  throw this.unimplemented('miNuevoMetodo no está soportado en web.');
}
```

**Paso 3 — Implementación Android en `WhatsAppSharePlugin.kt`:**

```kotlin
@PluginMethod
fun miNuevoMetodo(call: PluginCall) {
    val parametro = call.getString("parametro") ?: return call.reject("parametro es requerido")

    try {
        // lógica nativa aquí
        call.resolve()
    } catch (e: Exception) {
        call.reject("Error: ${e.message}", e)
    }
}
```

**Paso 4 — Compilar y empaquetar:**

```bash
npm run build
npm run pack:plugin
# → genera capacitor-whatsapp-share-X.X.X.tgz
```

### Publicar nueva versión en Git

```bash
# Actualizar versión en package.json
npm version patch   # 1.0.0 → 1.0.1
npm version minor   # 1.0.0 → 1.1.0
npm version major   # 1.0.0 → 2.0.0

# Subir cambios y tag al repositorio
git push origin main --tags
```

Los proyectos que usan el tag `#v1.0.1` deben actualizar con:

```bash
npm install git+https://github.com/tu-empresa/capacitor-whatsapp-share.git#v1.0.1
npx cap sync android
```
