package com.mueblesjamar.whatsappshare

import androidx.core.content.FileProvider

// Subclass vacía para evitar conflicto de nombre con otros FileProvider
// declarados en la app host (Capacitor, Ionic, etc.)
class WhatsAppShareFileProvider : FileProvider()
