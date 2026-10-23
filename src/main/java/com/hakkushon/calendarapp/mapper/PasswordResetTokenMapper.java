package com.hakkushon.calendarapp.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * password_reset_tokens テーブル用。
 * DBに入れるのは、メールのリンクに載せる値そのものではなく、そのSHA-256ハッシュ(64文字)。
 * (DBが見られても、そのまま使えるリンクは作れないようにするため)
 */
@Mapper
public interface PasswordResetTokenMapper {

    void insert(@Param("userId") Long userId, @Param("tokenHash") String tokenHash,
                @Param("expiresAt") LocalDateTime expiresAt);

    /** 未使用で期限内のトークンの持ち主。無ければnull。 */
    Long findValidUserId(@Param("tokenHash") String tokenHash);

    /** 未使用で期限内のときだけ「使用済み」にして1を返す。すでに使われた・期限切れなら0。 */
    int markUsed(@Param("tokenHash") String tokenHash);

    /** そのユーザーの未使用トークンをすべて無効にする(再発行したとき・パスワードを変えたとき)。 */
    void invalidateAllForUser(@Param("userId") Long userId);

    /** since 以降に発行された件数(連打でメールを送りすぎないための制限に使う)。 */
    int countIssuedSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
