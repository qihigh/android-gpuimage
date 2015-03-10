/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage.core;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

/**
 * Resembles a filter that consists of multiple filters applied after each
 * other.
 * 重新修改的groupFilter，去掉merge操作，重新管理frameBuffer
 */
public class GPUImageFilterGroup extends GPUImageFilter {

    protected List<GPUImageFilter> mFilters;
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;

    private final FloatBuffer mGLCubeBuffer;
    private final FloatBuffer mGLTextureBuffer;
    private final FloatBuffer mGLTextureFlipBuffer;

    /**
     * Instantiates a new GPUImageFilterGroup with no filters.
     */
    public GPUImageFilterGroup() {
        this(null);
    }

    /**
     * Instantiates a new GPUImageFilterGroup with the given filters.
     *
     * @param filters the filters which represent this filter
     */
    public GPUImageFilterGroup(List<GPUImageFilter> filters) {
        mFilters = new ArrayList<GPUImageFilter>();
        if (filters != null && filters.size() > 0) {
            mFilters.add(new GPUImageFilter());//前置保证，用于图片提前翻转
            mFilters = filters;
            mFilters.add(new GPUImageFilter());//后置保证，保证最后一个不是GroupFilter
        }

        mGLCubeBuffer = ByteBuffer.allocateDirect(GPUImageRenderer.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(GPUImageRenderer.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0);

        float[] flipTexture = TextureRotationUtil.getRotation(Rotation.NORMAL, false, true);
        mGLTextureFlipBuffer = ByteBuffer.allocateDirect(flipTexture.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureFlipBuffer.put(flipTexture).position(0);
    }

    public void addFilter(GPUImageFilter aFilter) {
        if (aFilter == null) {
            return;
        }
        if (mFilters.size() == 0) {
            mFilters.add(new GPUImageFilter());//前置保证，用于图片提前翻转
            mFilters.add(aFilter);
            mFilters.add(new GPUImageFilter());//后置保证，保证最后一个不是GroupFilter
        } else {
            mFilters.set(mFilters.size() - 1, aFilter);
            mFilters.add(new GPUImageFilter());//后置保证，保证最后一个不是GroupFilter
        }
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.core.GPUImageFilter#onInit()
     */
    @Override
    public void onInit() {
        super.onInit();
        for (GPUImageFilter filter : mFilters) {
            filter.init();
        }
    }

    /*
     * (non-Javadoc)
     * @see jp.co.cyberagent.android.gpuimage.core.GPUImageFilter#onDestroy()
     */
    @Override
    public void onDestroy() {
        destroyFramebuffers();
        for (GPUImageFilter filter : mFilters) {
            filter.destroy();
        }
        super.onDestroy();
    }

    private void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * jp.co.cyberagent.android.gpuimage.core.GPUImageFilter#onOutputSizeChanged(int,
     * int)
     */
    @Override
    public void onOutputSizeChanged(final int width, final int height) {
        super.onOutputSizeChanged(width, height);
        if (mFrameBuffers != null) {
            destroyFramebuffers();
        }

        for (GPUImageFilter filter : mFilters) {
            filter.onOutputSizeChanged(width, height);
        }

        //此处开始初始化frameBuffer，由Group本身管理frameBuffer修改为全局管理的frameBuffer
        if (getParent() == null) {//是root节点，生成所需数量的frameBuffer
            int size = getSize();

            mFrameBuffers = new int[size - 1];
            mFrameBufferTextures = new int[size - 1];

            GPUImageFilterGroupHelper.getInstance().setmFrameBuffers(mFrameBuffers);
            GPUImageFilterGroupHelper.getInstance().setmFrameBufferTextures(mFrameBufferTextures);


            for (int i = 0; i < size - 1; i++) {
                GLES20.glGenFramebuffers(1, mFrameBuffers, i);
                GLES20.glGenTextures(1, mFrameBufferTextures, i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i], 0);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }
    }

    /*
     * tips：Gpu处理过程中，图片会进行翻转，onDraw的时候，通过传入参数mGLCubeBuffer、mGLTextureFlipBuffer来进行修正。
     * 1.滤镜整体是一个树形结构，一定要从根节点触发渲染，否则frameBuffer、index等数据都是不对的。树才是一个可以正常工作的整体。
     * 2.树第一个叶子节点处理，保证是空的GPUImageFilter，用于保证图片进行了翻转了的。方便后续的处理。
     * 3.下一个树的节点如果是普通滤镜，需要提前声明将该滤镜处理的结构加入到frameBuffer中管理，对应的id存
     *  储到_FrameBuffers数组中，处理完之后恢复frameBuffer绑定到0，也就是不做frameBuffer绑定。然后从
     *  数组中得到该节点滤镜处理的结果texture，作为下一个滤镜处理的texture
     * 4.下一个树的节点如果是group滤镜，直接调用group的onDraw方法即可，因为其内部已经管理了frameBuffer的添加和释放。
     *  将group内最后一个滤镜处理的texture作为下一个滤镜处理的texture。
     * 5.最终的输出的时候，最后一个texture 不能绑定到frameBuffer中，因为最终输出的是GPU中正在渲染的图，而不是
     *  输出frameBuffer中的texture。
     *
     */
    @SuppressLint("WrongCall")
    @Override
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        runPendingOnDrawTasks();
        if (!isInitialized() || mFrameBuffers == null || mFrameBufferTextures == null) {
            return;
        }

        GPUImageFilterGroup root = getRoot();
        int[] _FrameBuffers = GPUImageFilterGroupHelper.getInstance().getFrameBuffers();
        int[] _FrameBufferTextures = GPUImageFilterGroupHelper.getInstance().getFrameBufferTextures();

        if (mFilters != null) {
            int size = mFilters.size();
            int previousTexture = textureId;
            //此处处理图片的时候，由于GPU处理图片的原因，图片会发生翻转，这里的处理逻辑是保证滤镜数组第一个是一个空的
            //GPUImageFilter，用于提前触发图片翻转。同时保证最后一个滤镜也是空滤镜，是为了方便处理，保证最后一个滤镜不是GPUImageGroup。
            for (int i = 0; i < size; i++) {
                GPUImageFilter filter = mFilters.get(i);
                boolean isNotLast = i < size - 1;


                if (filter.isGroup()) {//清理工作交给group自己做，group中处理的统一是filpBuffer，因为有前置滤镜保证图片已经被翻转。
                    //第一次处理之后，frameBuffer中得到的previoutTexure是倒立的图片，直接传入flipBuffer将图片纹理进行翻转再处理
                    //需要注意的是，虽然处理完显示出来是正的图片，但是存储到frameBuffer中的仍然是翻转的图片，不过后续传入的参数还是flipBuffer
                    filter.onDraw(previousTexture, cubeBuffer, mGLTextureFlipBuffer);
                    previousTexture = _FrameBufferTextures[filter.getIndex() + filter.getSize() - 1];
                    continue;
                }

                if (root.getSize() - 1 != filter.getIndex()) {//不是最后一个滤镜处理，将当前处理绑定到frameBuffer中。
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, _FrameBuffers[filter.getIndex()]);
                    GLES20.glClearColor(0, 0, 0, 0);
                }

                //处理非group的情况
                if (i == 0) {
                    //第一次处理的时候，rootFilter 图片是正的，使用传入的textureBuffer即，处理完之后frameBuffer中是倒立的图片
                    //如果不是rootFilter，此时frameBuffer中已经是倒立的图片，但是传入的参数就是flipBuffer，也就不需要做特殊处理了
                    filter.onDraw(previousTexture, cubeBuffer, textureBuffer);
                } else {
                    //第一次处理之后，frameBuffer中得到的previoutTexure是倒立的图片，直接传入flipBuffer将图片纹理进行翻转再处理
                    //处理完显示出来是正的图片，但是存储到frameBuffer中的仍然是翻转的图片。
                    filter.onDraw(previousTexture, cubeBuffer, mGLTextureFlipBuffer);
                }

                //做frameBuffer的清理工作
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                previousTexture = _FrameBufferTextures[filter.getIndex()];
            }
        }
    }

    /**
     * Gets the filters.
     *
     * @return the filters
     */
    public List<GPUImageFilter> getFilters() {
        return mFilters;
    }

    @Override
    public int getSize() {
        int size = 0;
        for (GPUImageFilter filter : mFilters) {
            size += filter.getSize();
        }
        return size;
    }

    public GPUImageFilterGroup getRoot() {
        GPUImageFilterGroup parent = this;
        while (null != parent.getParent()) {
            parent = parent.getParent();
        }
        return parent;
    }

    @Override
    public void setParent(GPUImageFilterGroup parent) {
        this.mParent = parent;
        for (GPUImageFilter filter : mFilters) {
            filter.setParent(this);
        }
    }

    public boolean isGroup() {
        return true;
    }

    @Override
    public void setIndex(int index) {
        super.setIndex(index);//group不计index，index计数只用在叶子滤镜上。
        for (GPUImageFilter filter : mFilters) {
            filter.setIndex(index);
            index += filter.getSize();
        }
    }
}
