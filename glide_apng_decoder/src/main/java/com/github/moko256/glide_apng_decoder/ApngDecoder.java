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

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;

import net.ellerton.japng.Png;
import net.ellerton.japng.argb8888.Argb8888BitmapSequence;
import net.ellerton.japng.error.PngException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by moko256 on 2017/10/28.
 *
 * @author moko256
 */

public class ApngDecoder implements ResourceDecoder<InputStream, Drawable> {
    @Override
    public boolean handles(InputStream source, Options options) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte [] buffer = new byte[1024];
        while(true) {
            int len = source.read(buffer);
            if(len < 0) {
                break;
            }
            bout.write(buffer, 0, len);
        }
        byte[] b = bout.toByteArray();
        return b[0] == (byte) 0x89 &&
                b[1] == (byte) 0x50 &&
                b[2] == (byte) 0x4E &&
                b[3] == (byte) 0x47 &&
                b[4] == (byte) 0x0D &&
                b[5] == (byte) 0x0A &&
                b[6] == (byte) 0x1A &&
                b[7] == (byte) 0x0A;
    }

    @Nullable
    @Override
    public Resource<Drawable> decode(InputStream source, int width, int height, Options options) throws IOException {
        try {
            Argb8888BitmapSequence sequence = Png.readArgb8888BitmapSequence(source);
            if (sequence.isAnimated()) {
                final AnimationDrawable drawable = new AnimationDrawable();
                for (Argb8888BitmapSequence.Frame frame : sequence.getAnimationFrames()) {
                    drawable.addFrame(
                            new BitmapDrawable(Bitmap.createBitmap(
                                    frame.bitmap.getPixelArray(),
                                    frame.bitmap.getWidth(),
                                    frame.bitmap.getHeight(),
                                    Bitmap.Config.ARGB_8888)
                            ),
                            frame.control.getDelayMilliseconds()
                    );
                }
                return new Resource<Drawable>() {
                    @Override
                    public Class<Drawable> getResourceClass() {
                        return Drawable.class;
                    }

                    @Override
                    public Drawable get() {
                        return drawable;
                    }

                    @Override
                    public int getSize() {
                        return drawable.getIntrinsicWidth() * drawable.getIntrinsicHeight() * 4 * drawable.getNumberOfFrames();
                    }

                    @Override
                    public void recycle() {

                    }
                };
            } else {
                final Drawable drawable = new BitmapDrawable(Bitmap.createBitmap(
                        sequence.defaultImage.getPixelArray(),
                        sequence.defaultImage.getWidth(),
                        sequence.defaultImage.getHeight(),
                        Bitmap.Config.ARGB_8888
                ));
                return new Resource<Drawable>() {
                    @Override
                    public Class<Drawable> getResourceClass() {
                        return Drawable.class;
                    }

                    @Override
                    public Drawable get() {
                        return drawable;
                    }

                    @Override
                    public int getSize() {
                        return drawable.getIntrinsicWidth() * drawable.getIntrinsicHeight() * 4;
                    }

                    @Override
                    public void recycle() {

                    }
                };
            }
        } catch (PngException e) {
            e.printStackTrace();
            return null;
        }
    }
}
