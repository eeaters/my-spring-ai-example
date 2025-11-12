email-example 我是要测试邮件的回复功能,本次测试的主要问题在于, 我需要在回复邮件的时候将原邮件进行携带.实现邮件的响应逻辑

## 邮箱
- 使用网易163邮箱
  - 账号: 
  - 密钥: 

## 需求明确
1. **回复逻辑**: 自动回复
2. **回复内容**: "很高兴收到你的回复,期待下次的合作"
3. **邮件查找范围**: 查找最近12小时内的邮件
4. **错误处理**: 直接抛出错误
5. **目标邮件**: 回复subject包含 `回复：关于单号:MBL2025111009 的货物提货时间协调请求` 的邮件

## 拉取邮件
- 使用IMAP进行邮件拉取
- 查找最近12小时内的邮件

## 回复邮件
- 需要将原先的邮件格式保留，包含邮件头信息
- 在原邮件基础上进行回复
- 回复格式: 新回复内容 + 分隔线 + 原始邮件(含邮件头)

## Project Structure
**EmailClient**: include pullEmail(PullEmailRequest req) and sendEmail(SendEmailRequest req)
**NetEaseEmailClient**: NetEase email implement