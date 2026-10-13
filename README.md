# halo-plugin-X

为 [halo-theme-X](https://github.com/IFIAnder/halo-theme-X) 提供个人主页的**自助扩展资料** —— 背景图、位置、个人网站。

> **主题不依赖本插件。** 不装它，主题照常工作，只是这几项没法在站内自己改；
> 装了之后，主题里会多出对应的编辑入口。这是刻意设计的：插件只增强，不成为前提。

## 这是什么

X（Twitter）的个人主页有一张背景图、一行位置、一个网站链接。Halo 的 `User` 资源里没有这些字段，
前台也没有修改它们的接口（官方用户中心只允许改显示名和简介）。

这个插件补上这块：

| 字段 | 存放位置 |
| --- | --- |
| 个人主页背景图 | `User.metadata.annotations["theme-x/cover"]` |
| 位置 | `User.metadata.annotations["theme-x/location"]` |
| 个人网站 | `User.metadata.annotations["website"]` |

数据直接挂在 `User` 的注解上，**不新建资源**。这样主题只要读注解就能用，两边不需要额外的接口约定 ——
也就是说，即使哪天你手动在后台给某个用户加一条 `theme-x/cover` 注解，主题立刻就能显示出封面图。

## 接口

```
GET  /apis/x.api.halo.run/v1alpha1/my-profile
PUT  /apis/x.api.halo.run/v1alpha1/my-profile
GET  /apis/x.api.halo.run/v1alpha1/profiles/{username}
```

`PUT` 的请求体：

```json
{ "cover": "https://.../cover.jpg", "location": "上海", "website": "https://example.com" }
```

字段语义：**`null` 表示不修改，空字符串表示删除**。所以只想改位置时，只传 `location` 即可。

`GET /my-profile` 需要登录（匿名返回 401）；`PUT` 永远只作用于**当前登录用户**，不接受指定用户名，
越权在那个层面上就不存在。

## 安装

到 [Releases](https://github.com/IFIAnder/halo-plugin-X/releases) 下载 jar，
在 Halo 后台 **插件 → 安装** 上传即可。

要求 Halo `>= 2.26.0`。安装后需要手动启用。

## 从源码构建

```bash
./gradlew build      # 产物在 build/libs/
```

需要 JDK 21。构建时会由官方 Gradle 插件把项目版本写入 `plugin.yaml` 的 `spec.version`，
所以源文件里不要手写这个字段。

## 授权

MIT。

本项目的绝大部分代码由 **WorkBuddy（模型 DeepSeek V4.1 Flash）** 生成，人工负责提出需求、审阅与验收。
