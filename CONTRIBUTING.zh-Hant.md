# 貢獻指南

[Contributing Guid (英文版)](CONTRIBUTING.md)

---

## Issue 提交規範

在您提交新的 Issue 前，請先搜尋現有的 Issue，以避免重複。

提交 Issue 時，請提供以下資訊：

* **標題：** 請簡要且具體地描述您的問題或建議。
* **重現步驟：** 如果是 Bug 回報，請提供詳細的重現步驟。這有助於我們更快地解決問題。
* **預期結果與實際結果：** 說明您預期會看到什麼，以及實際發生了什麼。
* **環境資訊：** 請註明您使用的瀏覽器、作業系統版本等相關資訊。

---

## 程式碼提交規範

### 1. 分支命名規範

請從 `main` 分支建立您的功能分支。我們建議以下分支命名格式：

* `feat/編號-簡短描述` (新功能)
* `fix/編號-簡短描述` (Bug 修正)
* `docs/編號-簡短描述` (文件變動，如 README 或其他說明文件)
* `chore/編號-簡短描述` (日常維護，如 CI/CD 設定、建置流程或輔助工具的變動)

**範例：**

* `feat/123-add-login-button`
* `docs/456-update-contributing-guide`
* `chore/789-update-build-config`

### 2. Commit 訊息規範

我們遵循 **[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/zh-hans/)** 規範，這有助於自動產生版本日誌和版本號。

#### 結構
每個 Commit 訊息都必須遵循以下結構：

```text
<type>(<scope>): <subject>

Why:
- 解釋變更的動機或「為什麼」要進行此變動。

What:
- 以列點方式列出關鍵的技術變更。

[可選的頁腳 (footer)]
```

* **`type` (類型):** 必需。說明 Commit 的主要目的（例如：`feat`, `fix`, `docs`, `chore`, `refactor`）。
* **`scope` (範圍):** 可選。說明本次變動影響的程式碼範圍。
* **`subject` (主題):** 必需。簡短描述本次變動。
* **`Why` 區段:** 必需。解釋變更背後的「原因」。
* **`What` 區段:** 必需。以列點方式列出技術變更。

#### 範例

**標準 Commit：**
```text
feat(auth): implement biometric login support

Why:
- To enhance security and provide a faster login experience for users with capable devices.

What:
- Integrated Android BiometricPrompt API.
- Added BiometricManager check in LoginViewModel.
- Created AuthRepository interface for biometric token storage.
```

**破壞性變更 (Breaking Change)：**
在 `<type>` 或 `<scope>` 後方加上 `!`，並在頁腳包含 `BREAKING CHANGE`。
```text
feat(api)!: migrate to GraphQL for user profiles

Why:
- Existing REST endpoints are deprecated and do not support the new nested profile data structure.

What:
- Removed UserProfileResponse.kt DTO.
- Added Apollo Kotlin client dependency.
- Implemented GetUserProfile.graphql query.

BREAKING CHANGE: All REST-based profile lookups will fail. Use the new GraphQL-based service instead.
```

### 3. Pull Request (PR) 指南

當您的功能完成並準備好合併時，請建立一個 Pull Request。

PR 標題必須遵循 **Conventional Commits** 規範，以便自動化工具能正確解析。標題格式為：

`<type>(<scope>): <subject> (#<issue 編號>)`

* `<type>` (類型)：指明變更的性質，例如：
  * `feat`：新增功能
  * `fix`：修復 Bug
  * `docs`：文件變更
  * `style`：程式碼風格變更（不影響邏輯）
  * `refactor`：重構程式碼
  * `perf`：性能優化

* `<scope>` (範圍，可選)：指明變更所影響的模組或範圍。
* `<subject>` (主題)：簡潔地描述變更內容。
* `(#<issue 編號>)`：明確連結到相關的 Issue。

**範例：** `feat(auth): 新增使用者登入功能 (#123)`

在 PR 描述中，請詳細說明：

* **相關的 Issue 編號：** 請連結相關的 Issue，例如 `Closes #123`。
* **變更內容：** 簡要說明您所做的變更。
* **測試方式：** 描述您如何測試這些變更。

在程式碼被合併前，至少需要一位 Code Reviewer 審核並通過。

---
