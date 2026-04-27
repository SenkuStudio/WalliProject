package com.walli.wallpaper.util

/**
 * Transforms a standard URL into a Cloudflare Resized Image URL.
 * Example: https://cdn.example.com/full/image.jpg -> https://cdn.example.com/cdn-cgi/image/width=400,quality=75,format=auto/full/image.jpg
 */
fun String.toResizedImageUrl(width: Int? = null, quality: Int = 75): String {
    if (!this.startsWith("http") || this.contains("/cdn-cgi/image/")) return this
    
    // We try to find where the path starts. Assuming the URL structure has a domain then the path.
    // For Cloudflare Image Resizing, we insert /cdn-cgi/image/params/ before the path.
    
    val urlParts = this.split("/", limit = 4)
    if (urlParts.size < 4) return this
    
    val domain = urlParts.take(3).joinToString("/")
    val path = "/" + urlParts[3]
    
    val params = mutableListOf<String>()
    width?.let { params.add("width=$it") }
    params.add("quality=$quality")
    params.add("format=auto")
    
    val paramString = params.joinToString(",")
    
    return "$domain/cdn-cgi/image/$paramString$path"
}
