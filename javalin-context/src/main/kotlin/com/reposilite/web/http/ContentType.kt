/*
 * Copyright (c) 2021 dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.reposilite.web.http

enum class ContentType(
    val extension: String,
    val mimeType: String,
    val isHumanReadable: Boolean = false
) {

    // Fallback list of basic mime types used by Maven repository
    // ~ https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types

    /* Text */

    TEXT_PLAIN("txt", "text/plain", true),
    TEXT_CSS("css", "text/css", true),
    TEXT_CSV("csv", "text/csv"),
    TEXT_HTM("htm", "text/html", true),
    TEXT_HTML("html", "text/html", true),
    TEXT_XML("xml", "text/xml", true),

    /* Image */

    IMAGE_ICO("ico", "image/vnd.microsoft.icon"),
    IMAGE_JPEG("jpeg", "image/jpeg"),
    IMAGE_JPG("jpg", "image/jpg"),
    IMAGE_PNG("png", "image/png"),
    IMAGE_TIF("tif", "image/tiff"),
    IMAGE_TIFF("tiff", "image/tiff"),

    /* Font */

    FONT_OTF("otf", "font/otf"),
    FONT_TTF("ttf", "font/ttf"),

    /* Application */

    APPLICATION_OCTET_STREAM("bin", "application/octet-stream"),
    APPLICATION_BZ("bz", "application/x-bzip"),
    APPLICATION_BZ2("bz2", "application/x-bzip2"),
    APPLICATION_CDN("cdn", "text/plain", true),
    APPLICATION_GZ("gz", "application/gzip"),
    APPLICATION_JS("js", "application/javascript", true),
    APPLICATION_JSON("json", "application/json", true),
    APPLICATION_MPKG("mpkg", "application/vnd.apple.installer+xml"),
    APPLICATION_JAR("jar", "application/java-archive"),
    APPLICATION_POM("pom", "application/xml", true),
    APPLICATION_RAR("rar", "application/vnd.rar"),
    APPLICATION_SH("sh", "application/x-sh", true),
    APPLICATION_TAR("tar", "application/x-tar"),
    APPLICATION_XHTML("xhtml", "application/xhtml+xml", true),
    APPLICATION_YAML("yaml", "application/yaml", true),
    APPLICATION_YML("yml", "application/yaml", true),
    APPLICATION_ZIP("zip", "application/zip"),
    APPLICATION_7Z("7z", "application/x-7z-compressed"),

    /* Other */

    MULTIPART_FORM_DATA("multipart/form-data", "multipart/form-data")

    ;

    companion object {

        /* Compile time constants - useful for annotations & as raw string values */

        const val PLAIN = "text/plain"
        const val HTML = "text/html"
        const val XML = "text/xml"
        const val OCTET_STREAM = "application/octet-stream"
        const val JAVASCRIPT = "application/javascript"
        const val JSON = "application/json"
        const val FORM_DATA = "multipart/form-data"

        fun getContentType(mimeType: String): ContentType? =
            values().find { it.mimeType.equals(mimeType, ignoreCase = true) }

        @JvmStatic
        fun getContentTypeByExtension(extensions: String): ContentType? =
            values().find { it.extension.equals(extensions, ignoreCase = true) }

        @JvmStatic
        fun getMimeTypeByExtension(extensions: String): String? =
            getContentTypeByExtension(extensions)?.mimeType

    }

}