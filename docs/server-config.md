# WorldComment 服务端配置

配置文件位于世界存档目录下 `world-comment/config.json`。首次加载时自动创建。

所有配置项均可通过环境变量覆盖，环境变量名为 `SUBNOTEICA_` + 字段名的 UPPER_SNAKE_CASE 形式（例如 `SUBNOTEICA_REDIS_URL`）。

---

## 基础配置

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `syncRole` | string | `"host"` | 多服同步角色。`host` 为主服务器，`subordinate` 为从服务器 |
| `redisUrl` | string | `""` | Redis 连接地址。留空则不启用多服同步 |
| `uplinkUrl` | string | `""` | Uplink 同步目标 URL。留空则不启用 |
| `uplinkAuthKey` | string | `""` | Uplink 认证密钥 |

---

## 可见性与权限

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `allowMarkerUsage` | string | `"creative"` | 允许放置标记物的条件。`op` / `creative` / `all` |
| `commentVisibilityCriteria` | string | `"preference"` | 评论可见性策略。`always` / `never` / `preference` |
| `markerVisibilityCriteria` | string | `"always"` | 标记物可见性策略。同上 |
| `defaultCommentVisibilityPreference` | boolean | `false` | 新玩家加入时的默认可见性偏好 |
| `screenshotKeyTriggersComment` | boolean | `true` | 截图快捷键是否触发评论界面 |

---

## 图片功能

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `imageGlobalKill` | boolean | `false` | 全局关闭图片显示 |

---

## imageVariants — 图片变体规格

控制上传时生成哪些分辨率/质量的图片变体。值为 JSON 对象。

```json
"imageVariants": {
  "archive": { "maxWidth": 0, "quality": 100, "lossless": true },
  "detail": { "maxWidth": 1920, "quality": 95 },
  "thumbnail": { "maxWidth": 256, "quality": 80 }
}
```

### 变体说明

| 变体 | 用途 | 是否必须 |
|------|------|----------|
| `archive` | 原始/归档级保存。省略则 source 直接按 detail 规格压缩 | 可选 |
| `detail` | 查看大图时使用 | 必须（有默认值） |
| `thumbnail` | 列表缩略图。省略则由 CDN 动态生成或回退到 detail | 可选 |

### VariantSpec 字段

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `maxWidth` | int | `0` | 最大宽度（像素）。`0` 表示不缩放 |
| `quality` | int | `95` | WebP 压缩质量 (1-100) |
| `lossless` | boolean | `false` | 是否使用无损压缩（忽略 quality） |

### 默认行为

若省略整个 `imageVariants`，等效于：

```json
{
  "detail": { "maxWidth": 1920, "quality": 95 },
  "thumbnail": { "maxWidth": 256, "quality": 80 }
}
```

即：不保留归档原图，source 按 detail 规格压缩，额外生成缩略图。

---

## imageUploadConfig — 图片上传服务

配置图片上传目标。值为 JSON 数组，每项描述一个上传器。客户端按数组顺序尝试，某个失败则回退到下一个。

不配置或留空数组时，默认使用 `local`（服务端本地存储）。

### 通用字段

所有上传器共享以下可选字段：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `service` | string | — | **必填**。上传器类型标识 |
| `id` | string | `"{service}-{index}"` | 上传器 ID，用于 CDN 配置关联。建议手动指定 |
| `filenameFormat` | string | `"{id}-{initiatorName}{.variant}"` | 上传文件名模板（不含扩展名，自动追加 `.webp`） |
| `cdnImageTransform` | string | `""` | CDN 图片变换模板。留空表示不使用 CDN 动态缩放 |
| `ignoreNativeThumbnail` | boolean | `false` | 忽略服务商返回的原生缩略图，强制使用自定义缩略图策略 |

---

### service: `local`

使用服务端本地文件系统存储。图片通过 Minecraft 网络协议传输。

无需额外配置字段。文件存储在世界目录 `world-comment/image/` 下，自动按日期分桶（`YYMM/DD/`）。

```json
{ "service": "local" }
```

存储路径示例：`world-comment/image/2607/31/0001a2b3c4d5e6f7-Steve.webp`

---

### service: `s3PreSigned`

通过 S3 兼容存储（AWS S3、Cloudflare R2、OSS 等）上传。服务端生成预签名 URL，客户端直传。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `s3Endpoint` | string | 否 | 自定义 S3 endpoint（非 AWS 时填写） |
| `s3Bucket` | string | 是 | 存储桶名称 |
| `s3Region` | string | 是 | 区域标识 |
| `s3AccessKeyId` | string | 是 | Access Key ID |
| `s3SecretAccessKey` | string | 是 | Secret Access Key |
| `cdnBaseUrl` | string | 是 | 访问 URL 前缀（CDN 域名或桶公开域名） |
| `pathFormat` | string | 否 | 对象路径模板。默认 `"{y}{m}/{d}/{id}-{initiatorName}{.variant}"` |

注：S3 上传器不使用 `filenameFormat` 配置项，如需修改路径，需配置 `pathFormat`。

```json
{
  "service": "s3PreSigned",
  "id": "r2-main",
  "s3Endpoint": "https://xxxx.r2.cloudflarestorage.com",
  "s3Bucket": "worldcomment",
  "s3Region": "auto",
  "s3AccessKeyId": "your-access-key",
  "s3SecretAccessKey": "your-secret-key",
  "cdnBaseUrl": "https://img.example.com",
  "cdnImageTransform": "/cdn-cgi/image/width={width},quality={quality}/{path}"
}
```

---

### service: `smms`

上传到 SM.MS 图床。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `apiUrl` | string | 否 | API 地址。默认 `https://smms.app/api/v2/upload` |
| `apiToken` | string | 是 | API Token |

```json
{
  "service": "smms",
  "id": "smms",
  "apiToken": "your-token-here"
}
```

---

### service: `lsky`

上传到 Lsky Pro 图床。支持原生缩略图。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `apiUrl` | string | 是 | Lsky Pro API 上传地址 |
| `apiToken` | string | 是 | Bearer Token |
| `strategyId` | int | 否 | 存储策略 ID |
| `albumId` | int | 否 | 相册 ID |

```json
{
  "service": "lsky",
  "id": "lsky",
  "apiUrl": "https://your-lsky.example.com/api/v1/upload",
  "apiToken": "your-bearer-token",
  "strategyId": 1
}
```

---

### service: `imgloc`

上传到 Chevereto 架构图床。支持原生缩略图。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `apiUrl` | string | 否 | API 地址。默认 `https://imgloc.com/api/1/upload` |
| `apiToken` | string | 是 | API Key |

```json
{
  "service": "imgloc",
  "id": "imgloc",
  "apiToken": "your-api-key"
}
```

---

## 文件名/路径模板变量

`filenameFormat`（HTTP 上传器）和 `pathFormat`（S3）使用以下模板变量：

| 变量 | 展开结果 | 示例 |
|------|----------|------|
| `{id}` | 评论 ID（16 位十六进制） | `0001a2b3c4d5e6f7` |
| `{variant}` | 变体标签 | `src` / `mid` / `thumb` |
| `{.variant}` | 带点的变体后缀（source 为空） | ` ` / `.mid` / `.thumb` |
| `{initiator}` | 玩家 UUID | `550e8400-e29b-...` |
| `{initiatorName}` | 玩家名（已过滤特殊字符） | `Steve` |
| `{Y}` | 四位年份 | `2026` |
| `{y}` | 两位年份 | `26` |
| `{m}` | 两位月份 | `07` |
| `{d}` | 两位日期 | `31` |
| `{timestamp}` | Unix 时间戳（秒） | `1785532800` |
| `{uniqid}` | 时间戳衍生唯一 ID | `6789abcd01234` |
| `{str-random-N}` | N 位随机字母数字串 | `{str-random-8}` → `aB3xZ9kQ` |

模板不包含文件扩展名，运行时自动追加 `.webp`。

### 示例

| 模板 | 生成结果（source 变体） |
|------|------------------------|
| `{id}-{initiatorName}{.variant}` | `0001a2b3c4d5e6f7-Steve.webp` |
| `{y}{m}/{d}/{id}-{initiatorName}{.variant}` | `2607/31/0001a2b3c4d5e6f7-Steve.webp` |
| 同上（thumbnail 变体） | `2607/31/0001a2b3c4d5e6f7-Steve.thumb.webp` |

---

## CDN 图片变换

`cdnImageTransform` 用于在显示时在 CDN 层生成不同尺寸的图片 URL，而只需存储一份。使用以下模板变量：

| 变量 | 说明 |
|------|------|
| `{path}` | 原始图片路径（不含前导 `/`） |
| `{width}` | 目标宽度（像素） |
| `{quality}` | 目标质量 (1-100) |
| `{quality_frac}` | 目标质量 (0.00-1.00) |

这项服务通常额外计费，请检查具体提供商的计费策略。

### 常见 CDN 示例

**Cloudflare Image Resizing：**
```
/cdn-cgi/image/width={width},quality={quality}/{path}
```

**阿里云 OSS 图片处理：**
```
/{path}?x-oss-process=image/resize,w_{width}/quality,q_{quality}
```

**腾讯云 CI 万象：**
```
/{path}?imageMogr2/thumbnail/{width}x/quality/{quality}
```

配置了 CDN 变换后，客户端将不再预先单独上传 thumbnail 和 detail（如果单独配置了 archive 级别清晰度）变体，节省存储空间。
---

## 完整配置示例

```json
{
  "syncRole": "host",
  "redisUrl": "",
  "allowMarkerUsage": "creative",
  "commentVisibilityCriteria": "preference",
  "markerVisibilityCriteria": "always",
  "screenshotKeyTriggersComment": true,
  "defaultCommentVisibilityPreference": false,
  "imageGlobalKill": false,
  "imageVariants": {
    "archive": { "maxWidth": 3840, "quality": 100, "lossless": true },
    "detail": { "maxWidth": 1920, "quality": 95 },
    "thumbnail": { "maxWidth": 256, "quality": 80 }
  },
  "imageUploadConfig": [
    {
      "service": "s3PreSigned",
      "id": "r2",
      "s3Endpoint": "https://account-id.r2.cloudflarestorage.com",
      "s3Bucket": "worldcomment-images",
      "s3Region": "auto",
      "s3AccessKeyId": "ACCESS_KEY",
      "s3SecretAccessKey": "SECRET_KEY",
      "cdnBaseUrl": "https://img.example.com",
      "pathFormat": "{Y}/{m}/{d}/{id}-{initiatorName}{.variant}",
      "cdnImageTransform": "/cdn-cgi/image/width={width},quality={quality}/{path}"
    }
  ]
}
```

此配置表示：
1. 原图以 lossless WebP 归档上传到 R2
2. 浏览时由 Cloudflare 动态缩放到 1920px/q95（大图）或 256px/q80（缩略图）
3. 无需额外存储 medium/thumbnail 文件
