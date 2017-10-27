/*
 * Copyright 2016 The twicalico authors
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

package com.github.moko256.glide_apng_decoder;

import android.support.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by moko256 on 2017/10/28.
 *
 * @author moko256
 */

public class ApngDecoder implements ResourceDecoder<InputStream, ApngDrawable> {
    @Override
    public boolean handles(InputStream source, Options options) throws IOException {
        return false;
    }

    @Nullable
    @Override
    public Resource<ApngDrawable> decode(InputStream source, int width, int height, Options options) throws IOException {
        return null;
    }
}
