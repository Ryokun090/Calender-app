# calendar-app セットアップ手順

## 1. MySQLの準備

1. ローカルにMySQLを立てる(バージョン8.0以降)
2. データベースを作成する

```sql
CREATE DATABASE calendar_app CHARACTER SET utf8mb4;
```

3. テーブルを作成する

```
src/main/resources/db/schema.sql を実行する
```

## 2. 接続設定

1. `src/main/resources/application-local.yml.example` をコピーして
   同じ場所に `application-local.yml` という名前で保存する
2. 中の`username` / `password`を自分のMySQLの認証情報に書き換える
3. このファイルは `.gitignore` 対象なのでコミットされない(各自バラバラでOK)

## 3. Eclipseへのインポート

1. File > Import > Maven > Existing Maven Projects
2. このフォルダ(`pom.xml`があるディレクトリ)を選択してFinish
3. 赤いエラーが出た場合はプロジェクト右クリック → Maven → Update Project

## 4. 起動確認

1. `CalendarAppApplication.java` を右クリック → Run As → Spring Boot App
   (または `Java Application` でも可)
2. コンソールに `Started CalendarAppApplication` と出れば起動成功
3. ブラウザで `http://localhost:8080/health` にアクセスし、`OK` と表示されればOK

## パッケージ構成

```
controller/  … 画面・APIの入り口
service/     … ビジネスロジック
mapper/      … MyBatisのマッパーインターフェース(対応するXMLは resources/mapper/ に置く)
domain/      … テーブルに対応するエンティティクラス
dto/         … 画面⇔Controller間のデータ受け渡し用クラス
common/      … 全ドメイン共通の部品(LoginUser, CalendarAuthService, SecurityConfigなど)
```

## 重要な共通ルール

- ログイン中ユーザーの取得は必ず `common.LoginUser` 経由で行うこと
- カレンダーの権限判定(owner/member)は必ず `common.CalendarAuthService` 経由で行うこと
- 詳しいチーム開発ルールは別途共有している「開発ガイドライン.md」を参照

## 注意

`common/SecurityConfig.java` は現時点では全リクエストを許可する暫定設定になっている。
認証機能の実装(担当A)が完了次第、正式な設定に差し替える。
