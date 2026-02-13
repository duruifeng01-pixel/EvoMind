# 03 后端接口设计（REST v1）

## 1. 认证与账号

- `POST /api/v1/auth/sms/send`
- `POST /api/v1/auth/sms/login`
- `POST /api/v1/auth/password/login`
- `POST /api/v1/auth/wechat/login`
- `POST /api/v1/auth/password/reset`
- `POST /api/v1/auth/logout`

## 2. 新手引导

- `GET /api/v1/onboarding/state?userId=...`
- `POST /api/v1/onboarding/complete?userId=...`（完成后发放7天体验权益）

## 3. 信息源导入

- `POST /api/v1/sources/ocr/recognize`（上传截图，返回候选博主）
- `POST /api/v1/sources/import`（勾选后一键导入）
- `POST /api/v1/sources/manual`（手动链接导入）
- `GET /api/v1/sources?userId=...`
- `DELETE /api/v1/sources/{id}?userId=...`

## 4. 认知与内化

- `GET /api/v1/cards/feed?userId=...`（7:3混合流）
- `GET /api/v1/cards/{id}/mindmap`
- `GET /api/v1/cards/{id}/drilldown?nodeId=...`（临时原文段落）
- `POST /api/v1/discussion/daily-question/generate`
- `POST /api/v1/discussion/{id}/reply`
- `POST /api/v1/discussion/{id}/finalize`

## 5. 任务与作品

- `GET /api/v1/challenges/current?userId=...`
- `POST /api/v1/challenges/{id}/status`
- `POST /api/v1/challenges/{id}/artifact`

## 6. 支付与订阅

- `GET /api/v1/subscription/plans`
- `POST /api/v1/subscription/cost-estimate`
- `POST /api/v1/orders/create`
- `POST /api/v1/pay/wechat/callback`
- `POST /api/v1/pay/alipay/callback`
- `GET /api/v1/orders/history?userId=...`
- `POST /api/v1/refund/apply`

## 7. 隐私与数据权利

- `POST /api/v1/privacy/export`
- `POST /api/v1/privacy/delete-account`

## 8. 通用返回

```json
{
  "code": 0,
  "message": "ok",
  "requestId": "trace-id",
  "data": {}
}
```
