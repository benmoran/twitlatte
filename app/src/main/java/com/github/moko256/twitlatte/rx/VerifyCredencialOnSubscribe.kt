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

package com.github.moko256.twitlatte.rx

import com.github.moko256.twitlatte.cacheMap.UserCacheMap
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import twitter4j.Twitter
import twitter4j.TwitterException
import twitter4j.User

/**
 * Created by moko256 on 2018/08/09.
 *
 * @author moko256
 */
class VerifyCredencialOnSubscribe(
        val client: Twitter,
        val cache: UserCacheMap,
        val userId: Long
): SingleOnSubscribe<User> {
    override fun subscribe(emitter: SingleEmitter<User>) {
        try {
            var me = cache.get(userId)
            if (me == null) {
                me = client.verifyCredentials()
                cache.add(me)
            }
            emitter.onSuccess(me)
        } catch (e: TwitterException) {
            emitter.tryOnError(e)
        }

    }
}