/*
 * Copyright 2015-2018 The twitlatte authors
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

package com.github.moko256.twitlatte.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.text.TextUtils
import com.github.moko256.twitlatte.array.ArrayUtils
import com.github.moko256.twitlatte.entity.*
import com.github.moko256.twitlatte.text.link.MTHtmlParser
import com.github.moko256.twitlatte.text.link.entity.Link
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by moko256 on 2017/03/17.
 *
 * @author moko256
 */

class CachedStatusesSQLiteOpenHelper(
        context: Context,
        val accessToken: AccessToken?
): SQLiteOpenHelper(
        context,
        if (accessToken != null) {
            File(context.cacheDir, accessToken.getKeyString() + "/CachedStatuses.db").absolutePath
        } else {
            null
        },
        null,
        /*BuildConfig.CACHE_DATABASE_VERSION*/4
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
                "create table " + TABLE_NAME + "(" + ArrayUtils.toCommaSplitString(TABLE_COLUMNS) + ", primary key(id))"
        )
        db.execSQL("create unique index idindex on $TABLE_NAME(id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("alter table $TABLE_NAME add column contentWarning")
        }

        if (oldVersion < 3) {
            db.execSQL("alter table $TABLE_NAME add column repliesCount")
        }

        if (oldVersion < 4) {
            val oldStatuses = OldCachedStatusesSQLiteOpenHelper.getCachedStatus(db)
            db.delete(TABLE_NAME, null, null)
            db.execSQL("alter table $TABLE_NAME add column (repeatedStatusId, sourceName, sourceWebsite, isRepeated, repeatCount, isSensitive, mentions, medias_urls, medias_types, emojis_shortcodes, emojis_urls, visibility)")
            db.execSQL("alter table $TABLE_NAME drop column (retweetedStatusId, isRetweeted, retweetCount, isPossiblySensitive, UserMentionEntity_texts, UserMentionEntity_ids, UserMentionEntity_texts, UserMentionEntity_ids, UserMentionEntity_names, UserMentionEntity_screenNames, UserMentionEntity_starts, UserMentionEntity_ends, URLEntity_texts, URLEntity_expandedURLs, URLEntity_displayURLs, URLEntity_starts, URLEntity_ends, HashtagEntity_texts, HashtagEntity_starts, HashtagEntity_ends, MediaEntity_texts, MediaEntity_expandedURLs, MediaEntity_displayURLs, MediaEntity_ids, MediaEntity_MediaURLs, MediaEntity_MediaURLHttpSs, MediaEntity_types, MediaEntity_Variants_bitrates, MediaEntity_Variants_contentTypes, MediaEntity_Variants_uris, MediaEntity_starts, MediaEntity_ends, SymbolEntity_texts, SymbolEntity_starts, SymbolEntity_ends, Emoji_shortcodes, Emoji_urls)")

            val contentValues = ArrayList<ContentValues>(oldStatuses.size)
            for (statusPair in oldStatuses) {
                val status = statusPair.first
                contentValues.add(createStatusContentValues(if (accessToken?.type == Type.MASTODON) {
                    val parsedSource = MTHtmlParser.convertToContentAndLinks(status.source)
                    val urls = MTHtmlParser.convertToContentAndLinks(status.text)

                    if (status.retweetedStatusId == -1L) {
                        Status(
                                id = status.id,
                                userId = status.user.id,
                                text = urls.first,
                                sourceName = parsedSource.first,
                                sourceWebsite = parsedSource.second.first().url,
                                createdAt = status.createdAt,
                                inReplyToStatusId = status.inReplyToStatusId,
                                inReplyToUserId = status.inReplyToUserId,
                                inReplyToScreenName = status.inReplyToScreenName,
                                isFavorited = status.isFavorited,
                                isRepeated = status.isRetweeted,
                                favoriteCount = status.favoriteCount,
                                repeatCount = status.retweetCount,
                                repliesCount = status.repliesCount,
                                isSensitive = status.isPossiblySensitive,
                                lang = status.lang,
                                medias = if (status.mediaEntities.isNotEmpty()) {
                                    status.mediaEntities.map {
                                        val resultUrl: String
                                        val type: String

                                        when(it.type) {
                                            "video" -> {
                                                resultUrl = it.videoVariants[0].url
                                                type = Media.ImageType.VIDEO_ONE.value
                                            }
                                            "animated_gif" -> {
                                                resultUrl = it.videoVariants[0].url
                                                type = Media.ImageType.GIF.value
                                            }
                                            else -> {
                                                resultUrl = it.mediaURLHttps
                                                type = Media.ImageType.PICTURE.value
                                            }
                                        }

                                        Media(
                                                url = resultUrl,
                                                imageType = type
                                        )
                                    }.toTypedArray()
                                } else {
                                    null
                                },
                                urls = urls.second,
                                emojis = status.emojis.toTypedArray(),
                                url = "https://twitter.com/" + status.user.screenName + "/status/" + status.id.toString(),
                                mentions = status.userMentionEntities.map {
                                    it.screenName
                                }.toTypedArray(),
                                spoilerText = status.spoilerText,
                                quotedStatusId = status.quotedStatusId,
                                visibility = null
                        )
                    } else {
                        Repeat(
                                id = status.id,
                                userId = status.user.id,
                                repeatedStatusId = status.retweetedStatusId,
                                createdAt = status.createdAt
                        )
                    }
                } else {
                    status.convertToCommonStatus()
                }).apply {
                    put("count", statusPair.second)
                })
            }

            for (values in contentValues) {
                db.insert(TABLE_NAME, null, values)
            }

        }
    }

    fun getCachedStatus(id: Long): StatusObject? {
        var status: StatusObject? = null

        synchronized(this) {
            val database = readableDatabase
            val c = database.query(
                    TABLE_NAME,
                    TABLE_COLUMNS,
                    "id=" + id.toString(), null, null, null, null, "1"
            )
            if (c.moveToLast()) {
                val createdAt = Date(c.getLong(0))
                val statusId = c.getLong(1)
                val userId = c.getLong(2)
                val repeatedStatusId = c.getLong(3)
                if (repeatedStatusId == -1L) {
                    status = Status(
                            createdAt = createdAt,
                            id = statusId,
                            userId = userId,
                            text = c.getString(4),
                            sourceName = c.getString(5),
                            sourceWebsite = c.getString(6),
                            inReplyToStatusId = c.getLong(7),
                            inReplyToUserId = c.getLong(8),
                            isFavorited = c.getInt(9) != 0,
                            isRepeated = c.getInt(10) != 0,
                            favoriteCount = c.getInt(11),
                            repeatCount = c.getInt(12),
                            repliesCount = c.getInt(13),
                            inReplyToScreenName = c.getString(14),
                            isSensitive = c.getInt(15) != 0,
                            lang = c.getString(16),
                            mentions = splitComma(c.getString(17)),
                            urls = restoreLinks(
                                    splitComma(c.getString(18)),
                                    splitComma(c.getString(19)),
                                    splitComma(c.getString(20))
                            ),
                            medias = restoreMedias(
                                    splitComma(c.getString(21)),
                                    splitComma(c.getString(22))
                            ),
                            quotedStatusId = c.getLong(23),
                            url = c.getString(24),
                            emojis = restoreEmojis(
                                    splitComma(c.getString(25)),
                                    splitComma(c.getString(26))
                            ),
                            spoilerText = c.getString(27),
                            visibility = c.getString(28)
                    )
                } else {
                    status = Repeat(
                            id = statusId,
                            userId = userId,
                            createdAt = createdAt,
                            repeatedStatusId = repeatedStatusId
                    )
                }
            }

            c.close()
            database.close()
        }

        return status
    }

    fun getIdsInUse(ids: List<Long>): List<Long> {
        val result = ArrayList<Long>(ids.size * 5)

        synchronized(this) {
            val database = readableDatabase
            for (id in ids) {
                val c = database.query(
                        TABLE_NAME,
                        arrayOf(TABLE_COLUMNS[1], TABLE_COLUMNS[3], TABLE_COLUMNS[44]),
                        "id=" + id.toString(), null, null, null, null
                )
                while (c.moveToNext()) {
                    val repeatId = c.getLong(1)
                    val quotedId = c.getLong(2)
                    if (repeatId != -1L) {
                        if (!result.contains(repeatId) && !ids.contains(repeatId)) {
                            result.add(repeatId)
                        }
                    } else if (quotedId != -1L) {
                        if (!result.contains(quotedId) && !ids.contains(quotedId)) {
                            result.add(quotedId)
                        }
                    }
                }
                c.close()
            }
            database.close()
        }

        if (result.size > 0) {
            result.addAll(getIdsInUse(result))
        }
        return result
    }

    fun addCachedStatus(status: StatusObject, incrementCount: Boolean) {
        val values = createStatusContentValues(status)

        synchronized(this) {
            val database = writableDatabase
            database.beginTransaction()
            database.replace(TABLE_NAME, null, values)
            if (incrementCount) {
                val statement = incrementCountStatement(database)
                statement.bindLong(1, status.getId())
                statement.execute()
            }
            database.setTransactionSuccessful()
            database.endTransaction()
            database.close()
        }
    }

    fun addCachedStatuses(statuses: Collection<StatusObject>, incrementCount: Boolean, vararg excludeIncrementIds: Long) {
        val contentValues = ArrayList<ContentValues>(statuses.size)
        for (status in statuses) {
            contentValues.add(createStatusContentValues(status))
        }

        synchronized(this) {
            val database = writableDatabase
            database.beginTransaction()
            val statement = if (incrementCount) incrementCountStatement(database) else null
            for (values in contentValues) {
                database.replace(TABLE_NAME, null, values)

                val id = values.getAsLong(TABLE_COLUMNS[1])
                if (incrementCount && (excludeIncrementIds.isEmpty() || !excludeIncrementIds.contains(id))) {
                    statement!!.bindLong(1, id!!)
                    statement.execute()
                }
            }
            database.setTransactionSuccessful()
            database.endTransaction()
            database.close()
        }
    }

    private fun incrementCountStatement(database: SQLiteDatabase): SQLiteStatement {
        return database.compileStatement("UPDATE $TABLE_NAME SET count=count+1 WHERE id=?")
    }

    private fun createStatusContentValues(status: StatusObject): ContentValues {
        val contentValues = ContentValues()

        when(status) {
            is Status -> {
                contentValues.put(TABLE_COLUMNS[0], status.createdAt.time)
                contentValues.put(TABLE_COLUMNS[1], status.id)
                contentValues.put(TABLE_COLUMNS[2], status.userId)
                contentValues.put(TABLE_COLUMNS[3], -1)
                contentValues.put(TABLE_COLUMNS[4], status.text)
                contentValues.put(TABLE_COLUMNS[5], status.sourceName)
                contentValues.put(TABLE_COLUMNS[6], status.sourceWebsite)
                contentValues.put(TABLE_COLUMNS[7], status.inReplyToStatusId)
                contentValues.put(TABLE_COLUMNS[8], status.inReplyToUserId)
                contentValues.put(TABLE_COLUMNS[9], status.isFavorited)
                contentValues.put(TABLE_COLUMNS[10], status.isRepeated)
                contentValues.put(TABLE_COLUMNS[11], status.favoriteCount)
                contentValues.put(TABLE_COLUMNS[12], status.repeatCount)
                contentValues.put(TABLE_COLUMNS[13], status.repliesCount)
                contentValues.put(TABLE_COLUMNS[14], status.inReplyToScreenName)
                contentValues.put(TABLE_COLUMNS[15], status.isSensitive)
                contentValues.put(TABLE_COLUMNS[16], status.lang)

                if (status.mentions != null) {
                    contentValues.put(TABLE_COLUMNS[17], ArrayUtils.toCommaSplitString(status.mentions).toString())
                }

                if (status.urls != null) {
                    val size = status.urls.size
                    val urls = arrayOfNulls<String>(size)
                    val starts = arrayOfNulls<String>(size)
                    val ends = arrayOfNulls<String>(size)

                    status.urls.forEachIndexed { i, entity ->
                        urls[i] = entity.url
                        starts[i] = entity.start.toString()
                        ends[i] = entity.end.toString()
                    }
                    contentValues.put(TABLE_COLUMNS[18], ArrayUtils.toCommaSplitString(urls).toString())
                    contentValues.put(TABLE_COLUMNS[19], ArrayUtils.toCommaSplitString(starts).toString())
                    contentValues.put(TABLE_COLUMNS[20], ArrayUtils.toCommaSplitString(ends).toString())
                }

                if (status.medias != null) {
                    val size = status.medias.size
                    val urls = arrayOfNulls<String>(size)
                    val types = arrayOfNulls<String>(size)

                    status.medias.forEachIndexed { i, entity ->
                        urls[i] = entity.url
                        types[i] = entity.imageType
                    }
                    contentValues.put(TABLE_COLUMNS[21], ArrayUtils.toCommaSplitString(urls).toString())
                    contentValues.put(TABLE_COLUMNS[22], ArrayUtils.toCommaSplitString(types).toString())
                }

                contentValues.put(TABLE_COLUMNS[23], status.quotedStatusId)

                contentValues.put(TABLE_COLUMNS[24], status.url)

                if (status.emojis != null) {
                    val size = status.emojis.size
                    val shortCodes = arrayOfNulls<String>(size)
                    val urls = arrayOfNulls<String>(size)

                    status.emojis.forEachIndexed { i, emoji ->
                        shortCodes[i] = emoji.shortCode
                        urls[i] = emoji.url
                    }
                    contentValues.put(TABLE_COLUMNS[25], ArrayUtils.toCommaSplitString(shortCodes).toString())
                    contentValues.put(TABLE_COLUMNS[26], ArrayUtils.toCommaSplitString(urls).toString())
                }
                contentValues.put(TABLE_COLUMNS[27], status.spoilerText)
                contentValues.put(TABLE_COLUMNS[28], status.visibility)
            }

            is Repeat -> {
                contentValues.put(TABLE_COLUMNS[0], status.createdAt.time)
                contentValues.put(TABLE_COLUMNS[1], status.id)
                contentValues.put(TABLE_COLUMNS[2], status.userId)
                contentValues.put(TABLE_COLUMNS[3], status.repeatedStatusId)
            }
        }

        return contentValues
    }

    fun deleteCachedStatus(id: Long) {
        synchronized(this) {
            val database = writableDatabase
            database.beginTransaction()

            val statement = decrementCountStatement(database)
            statement.bindLong(1, id)
            statement.execute()

            database.delete(TABLE_NAME, "count=0", null)
            database.setTransactionSuccessful()
            database.endTransaction()
            database.close()
        }
    }

    fun deleteCachedStatuses(ids: Collection<Long>) {
        synchronized(this) {
            val database = writableDatabase
            database.beginTransaction()
            val sqLiteStatement = decrementCountStatement(database)
            for (id in ids) {
                sqLiteStatement.bindLong(1, id)
                sqLiteStatement.execute()
            }
            database.delete(TABLE_NAME, "count=0", null)
            database.setTransactionSuccessful()
            database.endTransaction()
            database.close()
        }
    }

    private fun decrementCountStatement(database: SQLiteDatabase): SQLiteStatement {
        return database.compileStatement("UPDATE $TABLE_NAME SET count=count-1 WHERE id=?")
    }

    private fun splitComma(string: String?): Array<String>? {
        return if (!TextUtils.isEmpty(string)) {
            string!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else {
            null
        }
    }

    private fun parse(string: String?): Array<Array<String>?>? {
        if (TextUtils.isEmpty(string)) {
            return null
        }
        val resultA = string!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (resultA.size == 1 && resultA[0] == "") {
            return null
        }
        val result = arrayOfNulls<Array<String>>(resultA.size)
        for (i in resultA.indices) {
            result[i] = resultA[i].split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }
        return result
    }

    private fun restoreEmojis(
            shortCodes: Array<String>?,
            urls: Array<String>?
    ): Array<Emoji>? = if (shortCodes != null && urls != null && urls.size == shortCodes.size) {
        Array(shortCodes.size) {
            Emoji(
                    shortCode = shortCodes[it],
                    url = urls[it]
            )
        }
    } else {
        null
    }

    private fun restoreLinks(
            urls: Array<String>?,
            starts: Array<String>?,
            ends: Array<String>?
    ): Array<Link>? = if (urls != null
                && starts != null
                && starts.size == urls.size
                && ends != null
                && ends.size == urls.size) {
        Array(urls.size) {
            Link(
                    url = urls[it],
                    start = starts[it].toInt(),
                    end = ends[it].toInt()
            )
        }
    } else {
        null
    }

    private fun restoreMedias(
            urls: Array<String>?,
            imageTypes: Array<String>?
    ): Array<Media>? = if (urls != null && imageTypes != null && imageTypes.size == urls.size) {
        Array(urls.size) {
            Media(
                    url = urls[it],
                    imageType = imageTypes[it]
            )
        }
    } else {
        null
    }

    companion object {

        private val TABLE_NAME = "CachedStatuses"
        private val TABLE_COLUMNS = arrayOf(
                "createdAt",
                "id",
                "userId",
                "repeatedStatusId",
                "text",
                "sourceName",
                "sourceWebsite",
                "inReplyToStatusId",
                "inReplyToUserId",
                "isFavorited",
                "isRepeated",
                "favoriteCount",
                "repeatCount",
                "repliesCount",
                "inReplyToScreenName",
                "isSensitive",
                "lang",
                "mentions",
                "urls_urls",
                "urls_start",
                "urls_ends",
                "medias_urls",
                "medias_types",
                "quotedStatusId",
                "url",
                "emojis_shortcodes",
                "emojis_urls",
                "contentWarning",
                "visibility",
                "count"
        )
    }
}