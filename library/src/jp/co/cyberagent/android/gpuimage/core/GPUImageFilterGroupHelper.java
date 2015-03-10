package jp.co.cyberagent.android.gpuimage.core;

import android.opengl.GLES20;

/**
 * Created by qihuan on 3/10/15.
 */
public class GPUImageFilterGroupHelper {

    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;

    private static GPUImageFilterGroupHelper instance;

    private GPUImageFilterGroupHelper() {
    }

    public static GPUImageFilterGroupHelper getInstance() {
        if (instance == null) {
            instance = new GPUImageFilterGroupHelper();
        }
        return instance;
    }

    public int[] getFrameBufferTextures() {
        return mFrameBufferTextures;
    }

    public int[] getFrameBuffers() {
        return mFrameBuffers;
    }

    public void setmFrameBuffers(int[] mFrameBuffers) {
        this.mFrameBuffers = mFrameBuffers;
    }

    public void setmFrameBufferTextures(int[] mFrameBufferTextures) {
        this.mFrameBufferTextures = mFrameBufferTextures;
    }

    public void updateFilters(GPUImageFilterGroup filterGroup) {
        if (null == filterGroup.getFilters() || filterGroup.getFilters().size() == 0) {
            return;
        }
        int index = 0;//index指向的是每一个叶子滤镜
        for (GPUImageFilter filter : filterGroup.getFilters()) {
            //更新filter
            filter.setParent(filterGroup);
            filter.setIndex(index);
            index += filter.getSize();
        }
    }
}
