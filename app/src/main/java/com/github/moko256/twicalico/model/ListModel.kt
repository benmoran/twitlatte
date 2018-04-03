/*
 * Copyright 2018 The twicalico authors
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

package com.github.moko256.twicalico.model

import com.github.moko256.twicalico.repository.base.IdListRepository
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject

/**
 * Created by moko256 on 2018/04/03.
 */
class IdListModel(
        val idListRepository: IdListRepository
        ) {
    val list = ArrayList<Long>()

    val listTopAdded = PublishSubject.create<Int>()
    val listGapAdded = PublishSubject.create<IntRange>()
    val listBottomAdded = PublishSubject.create<Int>()

    fun requestTop(){
        idListRepository
                .request(since = list.first())
                .observeOn(Schedulers.io())
                .subscribe{
                    list.addAll(0, it!!)
                    listTopAdded.onNext(it.size)
                }
    }

    fun requestGap(position: Int){
        idListRepository
                .request(
                        since = list[if (list.size >= position + 2) position + 2 else position + 1],
                        max = list[position - 1] - 1L)
                .observeOn(Schedulers.io())
                .map {
                    val result = ArrayList(it)
                    if (result[result.size - 1] == result[position]) {
                        result.removeAt(result.size - 1)
                    } else {
                        result.add(-1L)
                    }
                    result
                }
                .subscribe{
                    list.addAll(position, it)
                    listGapAdded.onNext(position..position + it.size)
                }
    }

    fun requestBottom(){
        idListRepository
                .request(max = list.last())
                .observeOn(Schedulers.io())
                .subscribe{
                    list.addAll(it)
                    listBottomAdded.onNext(it.size)
                }
    }

    fun show(){

    }

    fun hide(){

    }
}