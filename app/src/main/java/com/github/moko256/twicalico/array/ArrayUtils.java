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

package com.github.moko256.twicalico.array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by moko256 on 2018/01/04.
 *
 * @author moko256
 */

public class ArrayUtils {
    @SafeVarargs
    public static <T> List<T> convertToList(T... array){
        return Arrays.asList(array);
    }

    public static List<Long> convertToLongList(long... array){
        List<Long> list = new ArrayList<>(array.length);
        for (long l : array) {
            list.add(l);
        }
        return list;
    }

    public static CharSequence toCommaSplitString(String[] array){
        int length = array.length;
        StringBuilder builder = new StringBuilder(length * 10);
        for (int i = 0; ; i++) {
            builder.append(array[i]);
            if (i < length - 1) {
                builder.append(",");
            } else {
                return builder;
            }
        }
    }
}
