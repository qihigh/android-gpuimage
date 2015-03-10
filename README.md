# GPUImage for Android
基于项目___修改的，因为原项目中的GPUImageGroup的处理不太好，这里打算进行重写。
原项目中GpuImageGroup的问题在于：处理过程中，Group内所有的滤镜全都拉到第一层进行处理，导致
    GpuImageGroup重写的onDraw方法不能被调用，但是如果不拉到第一层处理，结果会出问题。分析
    后发现是原项目中的frameBuffer部分管理的不好，这里打算重写GpuImageGroup，重新管理FrameBuffer
    的处理，使GpuImageGroup使用的更方便。
### TODO
    后期处理，想提出一个自己的GpuImageView，不采用glSurface，因为glSurfaceView不支持动画，
    不能加些特效处理。GpuImageView就是将bitmap丢给GpuImage进行处理，将返回的bitmap更新到
    imageView中去，性能上肯定会比glSurfaceView低。
## Requirements
* Android 2.2 or higher (OpenGL ES 2.0)

## Usage

### Gradle dependency
    去掉了maven的依赖

## License
    Copyright 2012 CyberAgent, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
