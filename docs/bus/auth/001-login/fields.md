# 字段设计

## 账号登录字段
| 字段 | 含义 |
|---|---|
| username | 用户名 |
| password | 密码 |
| captcha | 图形验证码 |
| captchaKey | 验证码键 |

## 手机号登录字段
| 字段 | 含义 |
|---|---|
| mobile | 手机号 |
| code | 短信验证码 |

## 扫码登录字段
| 字段 | 含义 |
|---|---|
| token | 二维码会话标识 |
| imageUrl | 二维码图片地址 |
| status | 扫码状态 |

## 页面状态字段
| 字段 | 含义 |
|---|---|
| activeTab | 当前登录方式 |
| needCaptcha | 是否要求图形验证码 |
| smsCountdown | 短信倒计时 |
| loading | 登录中状态 |
