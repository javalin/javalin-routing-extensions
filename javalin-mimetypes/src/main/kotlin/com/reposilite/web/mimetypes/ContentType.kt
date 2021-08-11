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

package com.reposilite.web.mimetypes

enum class ContentType(
    val extensions: String,
    val mimeType: String,
    val isHumanReadable: Boolean = false
) {

    // Fallback list of basic mime types used by Maven repository
    // ~ https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types

    /* Text */

    TEXT("txt", "text/plain", true),
    CSS("css", "text/css", true),
    CSV("csv", "text/csv"),
    HTM("htm", "text/html", true),
    HTML("html", "text/html", true),
    XML("xml", "text/plain", true),

    /* Image */

    ICO("ico", "image/vnd.microsoft.icon"),
    JPEG("jpeg", "image/jpeg"),
    JPG("jpg", "image/jpg"),
    PNG("png", "image/png"),
    TIF("tif", "image/tiff"),
    TIFF("tiff", "image/tiff"),

    /* Font */

    OTF("otf", "font/otf"),
    TTF("ttf", "font/ttf"),

    /* Application */

    BIN("bin", "application/octet-stream"),
    BZ("bz", "application/x-bzip"),
    BZ2("bz2", "application/x-bzip2"),
    CDN("cdn", "text/plain", true),
    GZ("gz", "application/gzip"),
    JS("js", "application/javascript", true),
    JSON("json", "application/json", true),
    MPKG("mpkg", "application/vnd.apple.installer+xml"),
    JAR("jar", "application/java-archive"),
    POM("pom", "application/xml", true),
    RAR("rar", "application/vnd.rar"),
    SH("sh", "application/x-sh", true),
    TAR("tar", "application/x-tar"),
    XHTML("xhtml", "application/xhtml+xml", true),
    YAML("yaml", "application/yaml", true),
    YML("yml", "application/yaml", true),
    ZIP("zip", "application/zip"),
    X7Z("7z", "application/x-7z-compressed")
    ;

    companion object {

        @JvmStatic
        fun getContentType(extensions: String): ContentType? =
            values().find { it.name.equals(extensions, ignoreCase = true) }

        @JvmStatic
        fun getContentType(extensions: String, defaultType: ContentType): ContentType =
            values().find { it.name.equals(extensions, ignoreCase = true) } ?: defaultType

        @JvmStatic
        fun getMimeType(extensions: String, defaultType: String = BIN.mimeType) =
            getContentType(extensions) ?: defaultType

    }

}