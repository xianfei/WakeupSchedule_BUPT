# WakeUp课程表BUPT

## 说明

该项目基于WakeUp课程表 3.612改编

修改部分：
- 加入北邮新教务系统支持
- 加入北邮两个的VPN校外导入课表支持
- 修改课程时间为北邮的45分钟参差不齐的课程时间
- 修改默认节数为14节
- 修改默认导航栏透明
- 修改默认壁纸及无课程提示
- 默认关闭自动更新及苏大生活


主要特性：
- 颜值很高；
- 自动导入课表，只要选课网上有课表就可以生成；
- 支持周数显示，并正确显示单双周课程；
- 简洁轻巧，没有广告不臃肿耗电少无后台，启动速度极快，生成课表后即点即看；
- 桌面小部件也做得很好看；
- 可以导出为日历格式。
- 支持Dark Mode

## 下载Apk

见Release

以下为原项目说明：

# WakeUp课程表 3.612

## 声明

开源旨在可以降低后来者的门槛，借鉴可以，但是希望在相关 App 中能有所声明。

## 上架情况

截至2020.02.10

- 酷安[√] 19万
- 应用宝[√] 12674
- 魅族应用商店[√] 21590
- 小米应用商店[√] 61799
- OPPO应用商店[√] 19.3万
- VIVO应用商店[√] 23万
- 华为应用商店[√] 51.9万

## 开源相关

### 集成的开源库

- AndroidX 项目
- [Kotlin](https://github.com/JetBrains/kotlin)
- [Material Design](https://github.com/material-components/material-components-android)
- [Retrofit2](https://github.com/square/retrofit)
- [Toasty](https://github.com/GrenderG/Toasty)
- [jsoup](https://github.com/jhy/jsoup)
- [NumberPickerView](https://github.com/Carbs0126/NumberPickerView)
- [BaseRecyclerViewAdapterHelper](https://github.com/CymChad/BaseRecyclerViewAdapterHelper)
- [ColorPicker](https://github.com/jaredrummler/ColorPicker)
- [glide-transformations](https://github.com/wasabeef/glide-transformations)
- [Glide](https://github.com/bumptech/glide)
- [Gson](https://github.com/google/gson)
- [MaterialFilePicker](https://github.com/nbsp-team/MaterialFilePicker)
- [Share2](https://github.com/baishixian/Share2/)
- [kotlin-csv](https://github.com/doyaaaaaken/kotlin-csv)
- [TextDrawable](https://github.com/jahirfiquitiva/TextDrawable)
- [Android-QuickSideBar](https://github.com/saiwu-bigkoo/Android-QuickSideBar/)
- [sticky-headers-recyclerview](https://github.com/timehop/sticky-headers-recyclerview)
- [biweekly](https://github.com/mangstadt/biweekly)
- [appcenter-sdk-android](https://github.com/microsoft/appcenter-sdk-android)

### 参考项目

苏大的正方教务模拟登录和课程解析部分参考了[另一个课程表项目](https://github.com/mnnyang/ClassSchedule)，不过我对课程解析部分改动非常大，导入更为准确。

## TODO

- 集成“咩咩”
- 支持课程笔记
- 直接写入系统日历
- ~~完善对方正教务课程的解析~~
- 适配已经提交数据的学校
- ~~数据备份和恢复（用课程文件导出导入实现了，还支持分享）~~
- ~~课程分享~~
- ~~增加对夏冬令时的支持（可以设置任意数量的时间表）~~
- 注册登录，小范围的社交，主要是为社团的活动服务
- 完全迁移至AndroidX
- 国际化

## License

```
Copyright 2019 YZune. https://github.com/YZune

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
 limitations under the License.
 ```