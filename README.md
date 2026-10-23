# calendar-app(共有カレンダーアプリ)

友人・サークル仲間との予定調整向けの共有カレンダー(TimeTreeを参考に、「一括予定登録」と「空き日程調整」を軸にしたもの)。
Google Calendar APIは使わず、カレンダー・予定・コメント・通知はすべて自前のMySQLで管理する。

- Java 17 / Spring Boot 3.3.4 / Spring Security
- MyBatis / MySQL 8.0以降 / Thymeleaf
- メール送信(2段階認証コード・パスワード再設定): JavaMailSender + Gmail SMTP

## 接続URL

ベースURL: `http://localhost:8080`

**入口 → http://localhost:8080/auth/login**
ログインに成功すると、最後に開いていたカレンダー(無ければ個人用カレンダー)に移動する。2段階認証をONにしたアカウントは、ログイン後に認証コード入力画面(`/auth/verify`)を挟む。

| 担当 | URL | 内容 |
|---|---|---|
| A 認証 | `/auth/login` | ログイン(「パスワードを忘れた方」のリンクあり) |
| | `/auth/signup` | 新規登録 |
| | `/auth/signup/2fa-prompt` | 新規登録直後の2段階認証の設定確認 |
| | `/auth/verify` | 認証コード(6桁)の入力 |
| | `/auth/password/forgot` | パスワード再設定メールの申し込み |
| | `/auth/password/reset?token=...` | メールのリンクから開く、新しいパスワードの入力 |
| | `/settings` | 設定(ニックネーム変更、2段階認証のオン・オフ) |
| | `/health` | 動作確認用(ログイン不要。`OK` と出る) |
| B カレンダー | `/calendars` | 最後に開いたカレンダー(無ければ個人用)へ移動するだけの入口 |
| | `/calendars/add` | カレンダー作成 / 招待コードで参加 |
| | `/calendars/{id}` | カレンダー詳細(月表示)。`?year=2026&month=10` で月を指定 |
| | `/calendars/{id}/members` | メンバー一覧、招待コードの発行・再発行、カレンダーの退出・削除 |
| C 予定 | `/calendars/{id}/schedules/add` | 予定の登録(日程を複数入れると一括登録) |
| | `/schedules/{id}` | 予定詳細(作成者・コメント欄を含む) |
| | `/schedules/{id}/edit` | 予定の編集 |
| D 空き日程 | `/calendars/{id}/free-slots/auto` | 空き日程の自動計算。出た候補をチェックして○×投票にできる |
| | `/calendars/{id}/free-slots` | ○×投票の一覧 |
| | `/calendars/{id}/free-slots/add` | 候補日程を手入力で作る |
| | `/polls/{id}` | 候補への○×回答 / 管理者による確定 |
| E 通知 | `/notifications` | 通知一覧(クリックで既読になり、該当の予定へ移動。「すべて既読にする」あり) |
| 共通 | `/search?q=...` | 予定・メンバーの検索(参加しているカレンダーの中だけ) |

フォームから呼ばれるPOST専用のURL(コメント投稿、削除、確定、退出など)は省略している。

## 1. MySQLの準備

1. ローカルにMySQLを立てる(バージョン8.0以降。開発ではXAMPPのMySQLを使用)
2. データベースを作成する

```sql
CREATE DATABASE calendar_app CHARACTER SET utf8mb4;
```

3. テーブルを作成する

```
src/main/resources/db/schema.sql を実行する
```

phpMyAdminを使う場合は、`calendar_app` を選んでSQLタブに `schema.sql` の全文を貼って実行する。

## 2. 接続設定

1. `src/main/resources/application-local.yml.example` をコピーして
   同じ場所に `application-local.yml` という名前で保存する
2. 中の`username` / `password`を自分のMySQLの認証情報に書き換える
3. メール送信用(2段階認証・パスワード再設定)に、Gmailの設定を追記する(`.example` に無い場合)

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: (送信に使うGmailアドレス)
    password: (Googleアカウントで発行したアプリパスワード)
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

4. このファイルは `.gitignore` 対象なのでコミットされない(各自バラバラでOK)

> YAMLのインデントは**スペースのみ**。タブ文字は使えない。
> 学校のネットワークでメール送信時に `PKIX path building failed` が出る場合は、開発時のみの回避策として
> `spring.mail.properties.mail.smtp.ssl.trust: smtp.gmail.com` を追加する(本番では使わない)。

## 3. Eclipseへのインポート

1. File > Import > Maven > Existing Maven Projects
2. このフォルダ(`pom.xml`があるディレクトリ)を選択してFinish
3. 赤いエラーが出た場合はプロジェクト右クリック → Maven → Update Project

## 4. 起動確認

1. `CalendarAppApplication.java` を右クリック → Run As → Spring Boot App
   (または `Java Application` でも可)
2. コンソールに `Started CalendarAppApplication` と出れば起動成功
3. ブラウザで `http://localhost:8080/health` にアクセスし、`OK` と表示されればOK

> コードを変更したら**アプリを再起動する**こと。再起動し忘れて「直っていない」となることが多い。

## 主な仕様

- **ログイン**:メールアドレス+パスワード。パスワードはBCryptでハッシュ化して保存
- **2段階認証**:ユーザーごとにON/OFFを選べる(新規登録直後、または設定画面から)。6桁コードをメールで送信。有効期限5分、誤入力5回でロック、再送は30秒空ける
- **パスワード再設定**:メールのリンク(有効30分、1回だけ使える)から新しいパスワードを設定する。DBにはリンクの値そのものではなくハッシュを保存。メールアドレスが登録されているかどうかは画面からは分からない。パスワードは8文字以上
- **個人用カレンダー**:新規登録時に自動で作られる(`is_personal` で共有カレンダーと区別)。招待・退出・削除はできない
- **招待コード**:6文字の英数字(紛らわしい文字は除外)、有効期限7日。コードを入力した人はその場でメンバーになる。再発行すると古いコードは使えなくなる
- **退出・削除**:メンバーは自分から共有カレンダーを抜けられる(抜けた人の○×回答は消え、作った予定・コメントは残る)。オーナーは抜けられず、共有カレンダーを削除できる(中の予定・コメント・調整はすべて消え、元に戻せない)
- **一括登録**:1回の保存で複数日程を登録。1件でも失敗したら全件ロールバック
- **空き日程(自動)**:期間・時間帯・必要な時間を指定すると、メンバー全員の予定を突き合わせ、集まれる人が多い日から順に表示する。計算には、各メンバーが参加している**すべてのカレンダー(個人用を含む)**の予定を使う(予定の中身は表示されない)。出た候補をチェックして、そのまま○×投票を作れる。「この日で決める」で、投票なしで予定を作ることもできる
- **空き日程(○×投票)**:候補を出す → メンバーが○×で回答 → カレンダーのオーナーが1つ選んで確定すると、予定として登録される
- **予定の表示**:月表示では、予定の帯が作成者ごとの色になり、作成者名が入る(色はメンバー一覧の並び順で決まる)
- **検索**:予定(タイトル・場所・説明)とメンバー(名前)。自分が参加しているカレンダーの中だけを探す
- **通知**:コメント投稿、予定の追加・変更・削除が、同じカレンダーの他メンバーに通知される(個人用カレンダーでは誰にも飛ばない)。コメントは500文字まで

## 使い方の注意

- **日をまたぐ予定**(例: 23:00〜翌1:00)は登録できない。「23:00〜23:59」と「翌日の0:00〜1:00」の2件に分けて登録する。
- 終了時刻に24:00は入れられない(23:59まで)。

## パッケージ構成

```
controller/  … 画面・APIの入り口
service/     … ビジネスロジック(トランザクションはここ)
mapper/      … MyBatisのマッパーインターフェース(対応するXMLは resources/mapper/ に置く)
domain/      … テーブルに対応するエンティティクラス
dto/         … 画面⇔Controller間のデータ受け渡し用クラス
common/      … 全ドメイン共通の部品(LoginUser, CalendarAuthService, SecurityConfigなど)
```

テンプレートは `src/main/resources/templates/` の下に、機能ごとのフォルダ(`auth/` `settings/` `calendars/` `schedules/` `freeslots/` `polls/` `notifications/` `search/` と、共通部品の `fragments/`)で置く。

## 重要な共通ルール

- ログイン中ユーザーの取得は必ず `common.LoginUser` 経由で行うこと(`@AuthenticationPrincipal LoginUser`)
- カレンダーの権限判定(owner/member)は必ず `common.CalendarAuthService` 経由で行うこと
- 詳しいチーム開発ルールは別途共有している「開発ガイドライン.md」を参照

## 未実装・既知の制限

- 空き日程で確定した予定を後から削除すると、調整側は「確定済み」のまま残り、「確定した予定は削除されました」と表示される(仕様)
- 翌日の予定のリマインド、出席確認、週・日表示、オーナーの交代は未実装
- ログイン失敗やパスワード再設定の試行回数制限は未実装。パスワードを変えても、すでにログイン中の端末はログアウトされない
- 空き日程の自動計算は、予定を入れていないメンバーを「いつでも空いている」と見なす
- 最後に開いたカレンダーはセッションに覚えているだけなので、ログアウトするとリセットされる

## DBの定義が変わった時

`schema.sql` が全員のDB構築の基準になる。更新があったら手元のDBも合わせること。
たとえば `poll_responses` は定義を変更したため、古い定義のDBでは空き日程の回答画面が `Unknown column` エラーになる。その場合は、次を実行してから `schema.sql` 内の新しい `CREATE TABLE poll_responses` を流す。

```sql
DROP TABLE IF EXISTS poll_responses;
```
